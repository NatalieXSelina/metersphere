INSERT INTO `bug_relation_case`(`id`, `case_id`, `bug_id`, `case_type`, `test_plan_id`, `test_plan_case_id`, `create_user`, `create_time`, `update_time`)
VALUES ('test-plan-bug-tmp-id', 'test-plan-bug-case-id', 'test-plan-bug-id', 'FUNCTIONAL', 'test-plan-id-for-bug', 'test-plan-bug-case-id', 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO `bug_relation_case`(`id`, `case_id`, `bug_id`, `case_type`, `test_plan_id`, `test_plan_case_id`, `create_user`, `create_time`, `update_time`)
VALUES ('test-plan-bug-id-1', 'test-plan-bug-case-id', 'bug-oasis-id-for-plan', 'FUNCTIONAL', 'test-plan-id-for-bug', 'test-plan-bug-case-id', 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO `bug_relation_case`(`id`, `case_id`, `bug_id`, `case_type`, `test_plan_id`, `test_plan_case_id`, `create_user`, `create_time`, `update_time`)
VALUES ('test-plan-bug-id-2', null, 'bug-oasis-id-for-plan', 'FUNCTIONAL', 'test-plan-id-for-bug', 'test-plan-bug-case-id', 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO bug (id, num, title, handle_users, handle_user, create_user, create_time,update_user, update_time, delete_user, delete_time, project_id, template_id, platform, status, tags, platform_bug_id, deleted, pos)
VALUES ('bug-oasis-id-for-plan', 100001, 'oasis', 'admin', 'admin', 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000, '100001100001', 'bug-template-id', 'Local', 'open', '["default-tag"]', null, 0, 5000);
INSERT INTO `test_plan`(`id`, `num`, `project_id`, `group_id`, `module_id`, `name`, `status`, `type`, `tags`, `create_time`, `create_user`, `update_time`, `update_user`, `planned_start_time`, `planned_end_time`, `actual_start_time`, `actual_end_time`, `description`)
VALUES ('test-plan-id-for-bug', 100001, '100001100001', 'NONE', '1', '测试一下计划-2', 'PREPARED', 'TEST_PLAN', NULL, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '11');
INSERT INTO `test_plan`(`id`, `num`, `project_id`, `group_id`, `module_id`, `name`, `status`, `type`, `tags`, `create_time`, `create_user`, `update_time`, `update_user`, `planned_start_time`, `planned_end_time`, `actual_start_time`, `actual_end_time`, `description`)
VALUES ('test-plan-id-for-bug-1', 100001, '100001100001', 'NONE', '1', '测试一下计划-3', 'PREPARED', 'TEST_PLAN', NULL, CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '11');