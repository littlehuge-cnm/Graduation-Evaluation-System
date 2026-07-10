USE graduation_evaluation;

ALTER TABLE t_score_record 
ADD COLUMN defense_record VARCHAR(2000) DEFAULT NULL COMMENT '答辩记录' AFTER comment;
