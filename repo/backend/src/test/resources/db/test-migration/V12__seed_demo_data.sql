-- V12: Seed demo data (H2 compatible - individual inserts)

-- Product Bundles
INSERT INTO product_bundles (name, description, price) VALUES ('Beginner Cooking Basics', 'Essential lessons for new home cooks.', 19.99);
INSERT INTO product_bundles (name, description, price) VALUES ('Italian Cuisine Masterclass', 'Comprehensive guide to authentic Italian cooking.', 39.99);
INSERT INTO product_bundles (name, description, price) VALUES ('Baking Fundamentals', 'Learn the science of baking.', 29.99);
INSERT INTO product_bundles (name, description, price) VALUES ('Asian Kitchen Essentials', 'Stir-fry, dumplings, curries, and noodle dishes.', 34.99);
INSERT INTO product_bundles (name, description, price) VALUES ('Advanced Pastry Arts', 'Choux, laminated doughs, chocolate work.', 49.99);

-- Audio Assets
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Knife Skills 101', 'Master basic knife cuts.', 1800, 1, 'BEGINNER', 'Technique', TRUE);
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Heat Control Fundamentals', 'Understanding your stovetop.', 2400, 1, 'BEGINNER', 'Technique', TRUE);
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Stock and Broth Basics', 'Building flavor foundations.', 1500, 1, 'BEGINNER', 'Basics', TRUE);
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Perfect Pasta from Scratch', 'Handmade pasta techniques.', 2700, 2, 'INTERMEDIATE', 'Italian', TRUE);
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Classic Italian Sauces', 'Five mother sauces.', 2100, 2, 'INTERMEDIATE', 'Italian', TRUE);
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Risotto Mastery', 'Perfect risotto techniques.', 1800, 2, 'INTERMEDIATE', 'Italian', TRUE);
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Bread Baking Basics', 'Yeast and gluten development.', 3000, 3, 'BEGINNER', 'Baking', TRUE);
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Cake Decorating Essentials', 'Frosting and piping.', 2400, 3, 'BEGINNER', 'Baking', TRUE);
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Stir-Fry Techniques', 'Wok skills and sauce building.', 1500, 4, 'INTERMEDIATE', 'Asian', TRUE);
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Dumpling Workshop', 'Dough, filling, and folding.', 2700, 4, 'INTERMEDIATE', 'Asian', TRUE);
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Seasonal Salads', 'Fresh salads with dressings.', 1200, NULL, 'BEGINNER', 'Basics', TRUE);
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES ('Egg Mastery', 'Complete egg techniques.', 1800, NULL, 'BEGINNER', 'Technique', TRUE);

-- Audio Segments
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (1, 0, 0, 1800000, '/audio/knife-skills.mp3', 28800000, 'abc123');
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (2, 0, 0, 2400000, '/audio/heat-control.mp3', 38400000, 'def456');
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (3, 0, 0, 1500000, '/audio/stock-broth.mp3', 24000000, 'ghi789');
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (4, 0, 0, 2700000, '/audio/pasta.mp3', 43200000, 'jkl012');
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (5, 0, 0, 2100000, '/audio/sauces.mp3', 33600000, 'mno345');
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (6, 0, 0, 1800000, '/audio/risotto.mp3', 28800000, 'pqr678');
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (7, 0, 0, 3000000, '/audio/bread.mp3', 48000000, 'stu901');
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (8, 0, 0, 2400000, '/audio/cake.mp3', 38400000, 'vwx234');
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (9, 0, 0, 1500000, '/audio/stirfry.mp3', 24000000, 'yza567');
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (10, 0, 0, 2700000, '/audio/dumpling.mp3', 43200000, 'bcd890');
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (11, 0, 0, 1200000, '/audio/salads.mp3', 19200000, 'efg123');
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES (12, 0, 0, 1800000, '/audio/eggs.mp3', 28800000, 'hij456');

-- Tip Cards
INSERT INTO tip_cards (title, short_text, detailed_text, scope, lesson_id, enabled, priority) VALUES ('Knife Safety', 'Always curl fingers when holding food.', 'The claw grip protects your fingers.', 'GLOBAL', NULL, TRUE, 10);
INSERT INTO tip_cards (title, short_text, detailed_text, scope, lesson_id, enabled, priority) VALUES ('Mise en Place', 'Prepare all ingredients before cooking.', 'French for everything in its place.', 'GLOBAL', NULL, TRUE, 9);
INSERT INTO tip_cards (title, short_text, detailed_text, scope, lesson_id, enabled, priority) VALUES ('Salt Pasta Water', 'Water should taste like the sea.', 'Add 1 tbsp salt per quart of water.', 'LESSON', 4, TRUE, 8);
INSERT INTO tip_cards (title, short_text, detailed_text, scope, lesson_id, enabled, priority) VALUES ('Rest Your Meat', 'Let meat rest 5-10 minutes.', 'Resting redistributes juices.', 'GLOBAL', NULL, TRUE, 7);
INSERT INTO tip_cards (title, short_text, detailed_text, scope, lesson_id, enabled, priority) VALUES ('Room Temp Eggs', 'Use room temp eggs for baking.', 'Cold eggs can curdle batter.', 'LESSON', 8, TRUE, 8);
INSERT INTO tip_cards (title, short_text, detailed_text, scope, lesson_id, enabled, priority) VALUES ('Taste As You Go', 'Adjust seasoning throughout.', 'Season in layers.', 'GLOBAL', NULL, TRUE, 6);
INSERT INTO tip_cards (title, short_text, detailed_text, scope, lesson_id, enabled, priority) VALUES ('Hot Wok Cold Oil', 'Heat wok first then add oil.', 'Prevents sticking and creates wok hei.', 'LESSON', 9, TRUE, 9);
INSERT INTO tip_cards (title, short_text, detailed_text, scope, lesson_id, enabled, priority) VALUES ('Windowpane Test', 'Test gluten with windowpane.', 'Stretch dough thin to see light.', 'LESSON', 7, TRUE, 8);

-- Tip Card Configurations
INSERT INTO tip_card_configurations (scope, scope_id, display_mode) VALUES ('GLOBAL', NULL, 'SHORT');
INSERT INTO tip_card_configurations (scope, scope_id, display_mode) VALUES ('LESSON', 4, 'DETAILED');
INSERT INTO tip_card_configurations (scope, scope_id, display_mode) VALUES ('LESSON', 7, 'DETAILED');
INSERT INTO tip_card_configurations (scope, scope_id, display_mode) VALUES ('LESSON', 9, 'SHORT');

-- Questions
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (1, 'What is the correct way to hold food when cutting?', 'SINGLE_CHOICE', 'EASY', 'claw grip', 'The claw grip keeps fingertips tucked.');
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (1, 'What knife cut produces matchstick-sized strips?', 'SINGLE_CHOICE', 'EASY', 'julienne', 'Julienne is 1/8 inch x 1/8 inch x 2 inches.');
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (1, 'What is a brunoise cut?', 'SINGLE_CHOICE', 'MEDIUM', 'small dice', 'A 1/8 inch cube from julienne strips.');
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (2, 'What temperature range is a simmer?', 'SINGLE_CHOICE', 'MEDIUM', '185-205', 'Gentle bubbles at 185-205F.');
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (2, 'What method uses high heat with little fat?', 'SINGLE_CHOICE', 'EASY', 'sauteing', 'High heat, small amount of fat.');
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (4, 'Ideal flour to egg ratio for pasta?', 'SINGLE_CHOICE', 'MEDIUM', '100g flour to 1 egg', 'Classic ratio: 100g flour per egg.');
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (4, 'How long should pasta dough rest?', 'SINGLE_CHOICE', 'EASY', '30 minutes', 'Allows gluten to relax.');
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (6, 'What rice for risotto?', 'SINGLE_CHOICE', 'EASY', 'arborio', 'High starch for creaminess.');
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (7, 'Purpose of kneading bread?', 'SINGLE_CHOICE', 'EASY', 'develop gluten', 'Creates elastic structure.');
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (7, 'Yeast activation temperature?', 'SINGLE_CHOICE', 'MEDIUM', '105-110', 'Warm enough to activate, not kill.');
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (9, 'First step in stir-frying?', 'SINGLE_CHOICE', 'EASY', 'heat the wok', 'Hot wok prevents sticking.');
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES (12, 'Ideal poaching temperature?', 'SINGLE_CHOICE', 'MEDIUM', '180-190', 'Gentle bubbles for intact eggs.');

-- Question Variants
INSERT INTO question_variants (original_question_id, question_text, canonical_answer, explanation) VALUES (1, 'Which grip protects fingers while cutting?', 'claw grip', 'Curled fingertips with knuckle guidance.');
INSERT INTO question_variants (original_question_id, question_text, canonical_answer, explanation) VALUES (6, 'How much flour per egg for pasta?', '100g', '100g flour to 1 large egg.');
INSERT INTO question_variants (original_question_id, question_text, canonical_answer, explanation) VALUES (8, 'Which rice is creamiest for risotto?', 'arborio', 'High starch for creamy texture.');

-- Question Similarity Links
INSERT INTO question_similarity_links (question_id_a, question_id_b, similarity_score) VALUES (1, 2, 0.7);
INSERT INTO question_similarity_links (question_id_a, question_id_b, similarity_score) VALUES (2, 3, 0.8);
INSERT INTO question_similarity_links (question_id_a, question_id_b, similarity_score) VALUES (4, 5, 0.5);
INSERT INTO question_similarity_links (question_id_a, question_id_b, similarity_score) VALUES (6, 7, 0.6);
INSERT INTO question_similarity_links (question_id_a, question_id_b, similarity_score) VALUES (9, 10, 0.7);
INSERT INTO question_similarity_links (question_id_a, question_id_b, similarity_score) VALUES (11, 5, 0.4);

-- Notebook Tags
INSERT INTO wrong_notebook_tags (label, is_admin_seeded) VALUES ('Technique Error', TRUE);
INSERT INTO wrong_notebook_tags (label, is_admin_seeded) VALUES ('Temperature Issue', TRUE);
INSERT INTO wrong_notebook_tags (label, is_admin_seeded) VALUES ('Timing Mistake', TRUE);
INSERT INTO wrong_notebook_tags (label, is_admin_seeded) VALUES ('Measurement Error', TRUE);
INSERT INTO wrong_notebook_tags (label, is_admin_seeded) VALUES ('Ingredient Confusion', TRUE);
INSERT INTO wrong_notebook_tags (label, is_admin_seeded) VALUES ('Method Misunderstanding', TRUE);
INSERT INTO wrong_notebook_tags (label, is_admin_seeded) VALUES ('Safety Concern', TRUE);
INSERT INTO wrong_notebook_tags (label, is_admin_seeded) VALUES ('Need More Practice', TRUE);
