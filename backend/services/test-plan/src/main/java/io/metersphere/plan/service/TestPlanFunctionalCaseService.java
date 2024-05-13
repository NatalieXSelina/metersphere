package io.metersphere.plan.service;

import io.metersphere.bug.dto.CaseRelateBugDTO;
import io.metersphere.bug.mapper.ExtBugRelateCaseMapper;
import io.metersphere.functional.domain.FunctionalCaseModule;
import io.metersphere.functional.dto.FunctionalCaseCustomFieldDTO;
import io.metersphere.functional.dto.FunctionalCaseModuleCountDTO;
import io.metersphere.functional.dto.FunctionalCaseModuleDTO;
import io.metersphere.functional.dto.ProjectOptionDTO;
import io.metersphere.functional.service.FunctionalCaseModuleService;
import io.metersphere.functional.service.FunctionalCaseService;
import io.metersphere.plan.domain.TestPlan;
import io.metersphere.plan.domain.TestPlanFunctionalCase;
import io.metersphere.plan.domain.TestPlanFunctionalCaseExample;
import io.metersphere.plan.dto.AssociationNodeSortDTO;
import io.metersphere.plan.dto.ResourceLogInsertModule;
import io.metersphere.plan.dto.TestPlanResourceAssociationParam;
import io.metersphere.plan.dto.request.BasePlanCaseBatchRequest;
import io.metersphere.plan.dto.request.ResourceSortRequest;
import io.metersphere.plan.dto.request.TestPlanCaseRequest;
import io.metersphere.plan.dto.response.TestPlanAssociationResponse;
import io.metersphere.plan.dto.response.TestPlanCasePageResponse;
import io.metersphere.plan.dto.response.TestPlanResourceSortResponse;
import io.metersphere.plan.mapper.ExtTestPlanFunctionalCaseMapper;
import io.metersphere.plan.mapper.ExtTestPlanModuleMapper;
import io.metersphere.plan.mapper.TestPlanFunctionalCaseMapper;
import io.metersphere.plan.mapper.TestPlanMapper;
import io.metersphere.project.domain.Project;
import io.metersphere.project.dto.ModuleCountDTO;
import io.metersphere.sdk.constants.TestPlanResourceConstants;
import io.metersphere.sdk.exception.MSException;
import io.metersphere.sdk.util.BeanUtils;
import io.metersphere.sdk.util.Translator;
import io.metersphere.system.dto.LogInsertModule;
import io.metersphere.system.dto.sdk.BaseTreeNode;
import io.metersphere.system.service.UserLoginService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
public class TestPlanFunctionalCaseService extends TestPlanResourceService {
    @Resource
    private TestPlanFunctionalCaseMapper testPlanFunctionalCaseMapper;
    @Resource
    private ExtTestPlanFunctionalCaseMapper extTestPlanFunctionalCaseMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private TestPlanResourceLogService testPlanResourceLogService;
    @Resource
    private TestPlanMapper testPlanMapper;
    @Resource
    private FunctionalCaseService functionalCaseService;
    @Resource
    private UserLoginService userLoginService;
    @Resource
    private ExtBugRelateCaseMapper bugRelateCaseMapper;
    @Resource
    private TestPlanModuleService testPlanModuleService;
    @Resource
    private ExtTestPlanModuleMapper extTestPlanModuleMapper;
    @Resource
    private FunctionalCaseModuleService functionalCaseModuleService;
    private static final String CASE_MODULE_COUNT_ALL = "all";

    @Override
    public int deleteBatchByTestPlanId(List<String> testPlanIdList) {
        TestPlanFunctionalCaseExample example = new TestPlanFunctionalCaseExample();
        example.createCriteria().andTestPlanIdIn(testPlanIdList);
        return testPlanFunctionalCaseMapper.deleteByExample(example);
    }


    @Override
    public void updatePos(String id, long pos) {
        extTestPlanFunctionalCaseMapper.updatePos(id, pos);
    }

    @Override
    public void refreshPos(String testPlanId) {
        List<String> functionalCaseIdList = extTestPlanFunctionalCaseMapper.selectIdByTestPlanIdOrderByPos(testPlanId);
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtTestPlanFunctionalCaseMapper batchUpdateMapper = sqlSession.getMapper(ExtTestPlanFunctionalCaseMapper.class);
        for (int i = 0; i < functionalCaseIdList.size(); i++) {
            batchUpdateMapper.updatePos(functionalCaseIdList.get(i), i * DEFAULT_NODE_INTERVAL_POS);
        }
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
    }


    public void deleteTestPlanResource(@Validated TestPlanResourceAssociationParam associationParam) {
        TestPlanFunctionalCaseExample testPlanFunctionalCaseExample = new TestPlanFunctionalCaseExample();
        testPlanFunctionalCaseExample.createCriteria().andIdIn(associationParam.getResourceIdList());
        testPlanFunctionalCaseMapper.deleteByExample(testPlanFunctionalCaseExample);
        //TODO:更新执行历史的删除状态为true
    }

    public TestPlanResourceSortResponse sortNode(ResourceSortRequest request, LogInsertModule logInsertModule) {
        TestPlanFunctionalCase dragNode = testPlanFunctionalCaseMapper.selectByPrimaryKey(request.getDragNodeId());
        TestPlan testPlan = testPlanMapper.selectByPrimaryKey(request.getTestPlanId());
        if (dragNode == null) {
            throw new MSException(Translator.get("test_plan.drag.node.error"));
        }
        TestPlanResourceSortResponse response = new TestPlanResourceSortResponse();
        AssociationNodeSortDTO sortDTO = super.getNodeSortDTO(
                request,
                extTestPlanFunctionalCaseMapper::selectDragInfoById,
                extTestPlanFunctionalCaseMapper::selectNodeByPosOperator
        );
        super.sort(sortDTO);
        response.setSortNodeNum(1);
        testPlanResourceLogService.saveSortLog(testPlan, request.getDragNodeId(), new ResourceLogInsertModule(TestPlanResourceConstants.RESOURCE_FUNCTIONAL_CASE, logInsertModule));
        return response;
    }


    public List<TestPlanCasePageResponse> getFunctionalCasePage(TestPlanCaseRequest request, boolean deleted) {
        List<TestPlanCasePageResponse> functionalCaseLists = extTestPlanFunctionalCaseMapper.getCasePage(request, deleted, request.getSortString());
        if (CollectionUtils.isEmpty(functionalCaseLists)) {
            return new ArrayList<>();
        }
        //处理自定义字段值
        return handleCustomFields(functionalCaseLists, request.getProjectId());
    }

    private List<TestPlanCasePageResponse> handleCustomFields(List<TestPlanCasePageResponse> functionalCaseLists, String projectId) {
        List<String> ids = functionalCaseLists.stream().map(TestPlanCasePageResponse::getCaseId).collect(Collectors.toList());
        Map<String, List<FunctionalCaseCustomFieldDTO>> collect = functionalCaseService.getCaseCustomFiledMap(ids, projectId);
        Set<String> userIds = extractUserIds(functionalCaseLists);
        Map<String, List<CaseRelateBugDTO>> bugListMap = getBugData(ids);
        Map<String, String> userMap = userLoginService.getUserNameMap(new ArrayList<>(userIds));
        functionalCaseLists.forEach(testPlanCasePageResponse -> {
            testPlanCasePageResponse.setCustomFields(collect.get(testPlanCasePageResponse.getCaseId()));
            testPlanCasePageResponse.setCreateUserName(userMap.get(testPlanCasePageResponse.getCreateUser()));
            testPlanCasePageResponse.setExecuteUserName(userMap.get(testPlanCasePageResponse.getExecuteUser()));
            if (bugListMap.containsKey(testPlanCasePageResponse.getCaseId())) {
                List<CaseRelateBugDTO> bugDTOList = bugListMap.get(testPlanCasePageResponse.getCaseId());
                testPlanCasePageResponse.setBugList(bugDTOList);
                testPlanCasePageResponse.setBugCount(bugDTOList.size());
            }
        });
        return functionalCaseLists;

    }

    private Map<String, List<CaseRelateBugDTO>> getBugData(List<String> ids) {
        List<CaseRelateBugDTO> bugList = bugRelateCaseMapper.getBugCountByIds(ids);
        return bugList.stream().collect(Collectors.groupingBy(CaseRelateBugDTO::getCaseId));
    }


    public Set<String> extractUserIds(List<TestPlanCasePageResponse> list) {
        return list.stream()
                .flatMap(testPlanCasePageResponse -> Stream.of(testPlanCasePageResponse.getUpdateUser(), testPlanCasePageResponse.getCreateUser(), testPlanCasePageResponse.getExecuteUser()))
                .collect(Collectors.toSet());
    }

    public List<BaseTreeNode> getTree(String testPlanId) {
        List<BaseTreeNode> returnList = new ArrayList<>();
        List<ProjectOptionDTO> rootIds = extTestPlanFunctionalCaseMapper.selectRootIdByTestPlanId(testPlanId);
        Map<String, List<ProjectOptionDTO>> projectRootMap = rootIds.stream().collect(Collectors.groupingBy(ProjectOptionDTO::getName));
        List<FunctionalCaseModuleDTO> functionalModuleIds = extTestPlanFunctionalCaseMapper.selectBaseByProjectIdAndTestPlanId(testPlanId);
        Map<String, List<FunctionalCaseModuleDTO>> projectModuleMap = functionalModuleIds.stream().collect(Collectors.groupingBy(FunctionalCaseModule::getProjectId));
        if (MapUtils.isEmpty(projectModuleMap)) {
            projectRootMap.forEach((projectId, projectOptionDTOList) -> {
                BaseTreeNode projectNode = new BaseTreeNode(projectId, projectOptionDTOList.get(0).getProjectName(), Project.class.getName());
                returnList.add(projectNode);
                BaseTreeNode defaultNode = functionalCaseModuleService.getDefaultModule(Translator.get("functional_case.module.default.name"));
                projectNode.addChild(defaultNode);
            });
            return returnList;
        }
        projectModuleMap.forEach((projectId, moduleList) -> {
            BaseTreeNode projectNode = new BaseTreeNode(projectId, moduleList.get(0).getProjectName(), Project.class.getName());
            returnList.add(projectNode);
            List<String> projectModuleIds = moduleList.stream().map(FunctionalCaseModule::getId).toList();
            List<BaseTreeNode> nodeByNodeIds = functionalCaseModuleService.getNodeByNodeIds(projectModuleIds);
            boolean haveVirtualRootNode = CollectionUtils.isEmpty(projectRootMap.get(projectId));
            List<BaseTreeNode> baseTreeNodes = functionalCaseModuleService.buildTreeAndCountResource(nodeByNodeIds, !haveVirtualRootNode, Translator.get("functional_case.module.default.name"));
            for (BaseTreeNode baseTreeNode : baseTreeNodes) {
                projectNode.addChild(baseTreeNode);
            }
        });
        return returnList;
    }


    public Map<String, Long> moduleCount(TestPlanCaseRequest request) {
        //查出每个模块节点下的资源数量。 不需要按照模块进行筛选
        request.setModuleIds(null);
        List<FunctionalCaseModuleCountDTO> projectModuleCountDTOList = extTestPlanFunctionalCaseMapper.countModuleIdByRequest(request, false);
        Map<String, List<FunctionalCaseModuleCountDTO>> projectCountMap = projectModuleCountDTOList.stream().collect(Collectors.groupingBy(FunctionalCaseModuleCountDTO::getProjectId));
        Map<String, Long> projectModuleCountMap = new HashMap<>();
        projectCountMap.forEach((projectId, moduleCountDTOList) -> {
            List<ModuleCountDTO> moduleCountDTOS = new ArrayList<>();
            for (FunctionalCaseModuleCountDTO functionalCaseModuleCountDTO : moduleCountDTOList) {
                ModuleCountDTO moduleCountDTO = new ModuleCountDTO();
                BeanUtils.copyBean(moduleCountDTO, functionalCaseModuleCountDTO);
                moduleCountDTOS.add(moduleCountDTO);
            }
            int sum = moduleCountDTOList.stream().mapToInt(FunctionalCaseModuleCountDTO::getDataCount).sum();
            Map<String, Long> moduleCountMap = getModuleCountMap(projectId, request.getTestPlanId(), moduleCountDTOS);
            moduleCountMap.forEach((k, v) -> {
                if (projectModuleCountMap.get(k) == null || projectModuleCountMap.get(k) == 0L) {
                    projectModuleCountMap.put(k, v);
                }
            });
            projectModuleCountMap.put(projectId, (long) sum);
        });
        //查出全部用例数量
        long allCount = extTestPlanFunctionalCaseMapper.caseCount(request, false);
        projectModuleCountMap.put(CASE_MODULE_COUNT_ALL, allCount);
        return projectModuleCountMap;
    }


    public Map<String, Long> getModuleCountMap(String projectId, String testPlanId, List<ModuleCountDTO> moduleCountDTOList) {
        //构建模块树，并计算每个节点下的所有数量（包含子节点）
        List<BaseTreeNode> treeNodeList = this.getTreeOnlyIdsAndResourceCount(projectId, testPlanId, moduleCountDTOList);

        //通过广度遍历的方式构建返回值
        return testPlanModuleService.getIdCountMapByBreadth(treeNodeList);
    }

    public List<BaseTreeNode> getTreeOnlyIdsAndResourceCount(String projectId, String testPlanId, List<ModuleCountDTO> moduleCountDTOList) {
        //节点内容只有Id和parentId
        List<String> moduleIds = extTestPlanModuleMapper.selectIdByProjectIdAndTestPlanId(projectId, testPlanId);
        List<BaseTreeNode> nodeByNodeIds = testPlanModuleService.getNodeByNodeIds(moduleIds);
        return testPlanModuleService.buildTreeAndCountResource(nodeByNodeIds, moduleCountDTOList, true, Translator.get("functional_case.module.default.name"));


    }

    public TestPlanAssociationResponse disassociate(BasePlanCaseBatchRequest request, LogInsertModule logInsertModule) {
        List<String> selectIds = doSelectIds(request);
        return super.disassociate(
                TestPlanResourceConstants.RESOURCE_FUNCTIONAL_CASE,
                request,
                logInsertModule,
                selectIds,
                this::deleteTestPlanResource);
    }

    private List<String> doSelectIds(BasePlanCaseBatchRequest request) {
        if (request.isSelectAll()) {
            List<String> ids = extTestPlanFunctionalCaseMapper.getIds(request, false);
            if (CollectionUtils.isNotEmpty(request.getExcludeIds())) {
                ids.removeAll(request.getExcludeIds());
            }
            return ids;
        } else {
            return request.getSelectIds();
        }
    }
}
