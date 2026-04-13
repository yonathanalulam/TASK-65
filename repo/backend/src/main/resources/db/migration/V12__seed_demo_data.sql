-- V12: Seed demo data - audio assets, questions, bundles, tip cards

-- Product Bundles
INSERT INTO product_bundles (name, description, price) VALUES
    ('Beginner Cooking Basics', 'Essential lessons for new home cooks covering knife skills, heat control, and basic techniques.', 19.99),
    ('Italian Cuisine Masterclass', 'Comprehensive guide to authentic Italian cooking: pasta, sauces, risotto, and more.', 39.99),
    ('Baking Fundamentals', 'Learn the science of baking: breads, pastries, cakes, and decorating techniques.', 29.99),
    ('Asian Kitchen Essentials', 'Stir-fry, dumplings, curries, and noodle dishes from across Asia.', 34.99),
    ('Advanced Pastry Arts', 'Choux, laminated doughs, chocolate work, and plated desserts.', 49.99);

-- Audio Assets (lessons)
INSERT INTO audio_assets (title, description, duration_seconds, bundle_id, difficulty, category, active) VALUES
    ('Knife Skills 101', 'Master the basic knife cuts: julienne, brunoise, chiffonade, and more.', 1800, 1, 'BEGINNER', 'Technique', TRUE),
    ('Heat Control Fundamentals', 'Understanding your stovetop: searing, sauteing, simmering, and braising.', 2400, 1, 'BEGINNER', 'Technique', TRUE),
    ('Stock and Broth Basics', 'Building flavor foundations with homemade stocks.', 1500, 1, 'BEGINNER', 'Basics', TRUE),
    ('Perfect Pasta from Scratch', 'Handmade pasta dough, shaping, and cooking techniques.', 2700, 2, 'INTERMEDIATE', 'Italian', TRUE),
    ('Classic Italian Sauces', 'The five mother sauces of Italian cooking.', 2100, 2, 'INTERMEDIATE', 'Italian', TRUE),
    ('Risotto Mastery', 'Techniques for creamy, perfectly cooked risotto.', 1800, 2, 'INTERMEDIATE', 'Italian', TRUE),
    ('Bread Baking Basics', 'Yeast, gluten development, and your first loaf.', 3000, 3, 'BEGINNER', 'Baking', TRUE),
    ('Cake Decorating Essentials', 'Frosting, piping, and basic decoration techniques.', 2400, 3, 'BEGINNER', 'Baking', TRUE),
    ('Stir-Fry Techniques', 'Wok skills, oil temperature, and sauce building.', 1500, 4, 'INTERMEDIATE', 'Asian', TRUE),
    ('Dumpling Workshop', 'Dough making, filling, folding, and cooking methods.', 2700, 4, 'INTERMEDIATE', 'Asian', TRUE),
    ('Seasonal Salads', 'Fresh, vibrant salads with homemade dressings.', 1200, NULL, 'BEGINNER', 'Basics', TRUE),
    ('Egg Mastery', 'From poached to souffle: complete egg techniques.', 1800, NULL, 'BEGINNER', 'Technique', TRUE);

-- Audio Segments (one per asset for simplicity)
INSERT INTO audio_segments (audio_asset_id, segment_index, start_offset_ms, end_offset_ms, file_path, file_size_bytes, checksum) VALUES
    (1, 0, 0, 1800000, '/audio/knife-skills-101.mp3', 28800000, 'abc123def456'),
    (2, 0, 0, 2400000, '/audio/heat-control.mp3', 38400000, 'def456abc789'),
    (3, 0, 0, 1500000, '/audio/stock-broth.mp3', 24000000, 'ghi789jkl012'),
    (4, 0, 0, 2700000, '/audio/pasta-scratch.mp3', 43200000, 'jkl012mno345'),
    (5, 0, 0, 2100000, '/audio/italian-sauces.mp3', 33600000, 'mno345pqr678'),
    (6, 0, 0, 1800000, '/audio/risotto.mp3', 28800000, 'pqr678stu901'),
    (7, 0, 0, 3000000, '/audio/bread-baking.mp3', 48000000, 'stu901vwx234'),
    (8, 0, 0, 2400000, '/audio/cake-decorating.mp3', 38400000, 'vwx234yza567'),
    (9, 0, 0, 1500000, '/audio/stir-fry.mp3', 24000000, 'yza567bcd890'),
    (10, 0, 0, 2700000, '/audio/dumpling.mp3', 43200000, 'bcd890efg123'),
    (11, 0, 0, 1200000, '/audio/seasonal-salads.mp3', 19200000, 'efg123hij456'),
    (12, 0, 0, 1800000, '/audio/egg-mastery.mp3', 28800000, 'hij456klm789');

-- Tip Cards
INSERT INTO tip_cards (title, short_text, detailed_text, scope, lesson_id, enabled, priority) VALUES
    ('Knife Safety', 'Always curl fingers when holding food.', 'The claw grip protects your fingers: curl your fingertips under, using your knuckles as a guide for the blade. Never cut toward your body.', 'GLOBAL', NULL, TRUE, 10),
    ('Mise en Place', 'Prepare all ingredients before cooking.', 'French for "everything in its place." Measure, wash, chop, and organize all ingredients before turning on the heat. This prevents mistakes and burned food.', 'GLOBAL', NULL, TRUE, 9),
    ('Salt Your Pasta Water', 'Water should taste like the sea.', 'Add about 1 tablespoon of salt per quart of water. Salt the water after it boils but before adding pasta. This is your only chance to season the pasta itself.', 'LESSON', 4, TRUE, 8),
    ('Rest Your Meat', 'Let meat rest 5-10 minutes after cooking.', 'Resting allows juices to redistribute throughout the meat. Cut too early and juices run out, leaving dry meat. Tent loosely with foil while resting.', 'GLOBAL', NULL, TRUE, 7),
    ('Room Temperature Eggs', 'Use room temp eggs for baking.', 'Cold eggs can cause batter to curdle. Remove eggs from refrigerator 30 minutes before use, or warm them in a bowl of warm water for 5 minutes.', 'LESSON', 8, TRUE, 8),
    ('Taste As You Go', 'Adjust seasoning throughout cooking.', 'Season in layers rather than all at once. Taste at each stage and adjust. Remember you can always add more salt, but you cannot take it away.', 'GLOBAL', NULL, TRUE, 6),
    ('Hot Wok, Cold Oil', 'Heat wok first, then add oil.', 'A properly heated wok prevents sticking. Heat until water droplets dance and evaporate. Then add oil and swirl to coat. Food should sizzle immediately on contact.', 'LESSON', 9, TRUE, 9),
    ('Bread Dough Windowpane', 'Test gluten development with the windowpane test.', 'Stretch a small piece of dough gently between your fingers. If it stretches thin enough to see light through without tearing, gluten is fully developed.', 'LESSON', 7, TRUE, 8);

-- Tip Card Configurations
INSERT INTO tip_card_configurations (scope, scope_id, display_mode) VALUES
    ('GLOBAL', NULL, 'SHORT'),
    ('LESSON', 4, 'DETAILED'),
    ('LESSON', 7, 'DETAILED'),
    ('LESSON', 9, 'SHORT');

-- Questions (linked to lessons)
INSERT INTO questions (lesson_id, question_text, question_type, difficulty, canonical_answer, explanation) VALUES
    (1, 'What is the correct way to hold food when cutting with a knife?', 'SINGLE_CHOICE', 'EASY', 'claw grip', 'The claw grip keeps fingertips tucked under, using knuckles as a blade guide to prevent cuts.'),
    (1, 'What knife cut produces thin matchstick-sized strips?', 'SINGLE_CHOICE', 'EASY', 'julienne', 'Julienne cuts are approximately 1/8 inch x 1/8 inch x 2 inches, resembling matchsticks.'),
    (1, 'What is a brunoise cut?', 'SINGLE_CHOICE', 'MEDIUM', 'small dice', 'A brunoise is a 1/8 inch cube, created by first cutting julienne strips then cross-cutting into cubes.'),
    (2, 'What temperature range is considered a simmer?', 'SINGLE_CHOICE', 'MEDIUM', '185-205', 'A simmer is between 185-205 degrees F, with gentle bubbles breaking the surface occasionally.'),
    (2, 'What cooking method uses very high heat with a small amount of fat?', 'SINGLE_CHOICE', 'EASY', 'sauteing', 'Sauteing uses high heat and a small amount of fat to quickly cook food while developing color.'),
    (4, 'What is the ideal ratio of flour to eggs for fresh pasta?', 'SINGLE_CHOICE', 'MEDIUM', '100g flour to 1 egg', 'The classic ratio is approximately 100g of flour per large egg, yielding a firm but workable dough.'),
    (4, 'How long should fresh pasta dough rest before rolling?', 'SINGLE_CHOICE', 'EASY', '30 minutes', 'Resting for 30 minutes allows gluten to relax, making the dough easier to roll thin.'),
    (6, 'What type of rice is traditionally used for risotto?', 'SINGLE_CHOICE', 'EASY', 'arborio', 'Arborio rice has high starch content that creates the characteristic creamy texture when slowly cooked.'),
    (7, 'What is the purpose of kneading bread dough?', 'SINGLE_CHOICE', 'EASY', 'develop gluten', 'Kneading aligns and strengthens gluten strands, creating the elastic structure that traps gas and gives bread its texture.'),
    (7, 'What temperature water activates yeast without killing it?', 'SINGLE_CHOICE', 'MEDIUM', '105-110', 'Water at 105-110 degrees F is warm enough to activate yeast but cool enough not to kill it.'),
    (9, 'What is the first step in stir-frying?', 'SINGLE_CHOICE', 'EASY', 'heat the wok', 'Always heat the wok until smoking hot before adding oil. A hot wok prevents sticking and creates the desired wok hei.'),
    (12, 'What is the ideal water temperature for poaching eggs?', 'SINGLE_CHOICE', 'MEDIUM', '180-190', 'Water at 180-190 degrees F produces gentle bubbles ideal for poaching. A rolling boil will break the egg apart.');

-- Question Variants
INSERT INTO question_variants (original_question_id, question_text, canonical_answer, explanation) VALUES
    (1, 'Which grip technique protects your fingers while cutting?', 'claw grip', 'The claw grip uses curled fingertips and knuckle guidance.'),
    (6, 'For handmade pasta, how much flour do you need per egg?', '100g', 'Standard ratio is 100g flour to 1 large egg.'),
    (8, 'Which rice variety produces the creamiest risotto?', 'arborio', 'Arborio high starch content creates creamy texture.');

-- Question Similarity Links
INSERT INTO question_similarity_links (question_id_a, question_id_b, similarity_score) VALUES
    (1, 2, 0.7),
    (2, 3, 0.8),
    (4, 5, 0.5),
    (6, 7, 0.6),
    (9, 10, 0.7),
    (11, 5, 0.4);

-- Notebook Tags (admin-seeded)
INSERT INTO wrong_notebook_tags (label, is_admin_seeded) VALUES
    ('Technique Error', TRUE),
    ('Temperature Issue', TRUE),
    ('Timing Mistake', TRUE),
    ('Measurement Error', TRUE),
    ('Ingredient Confusion', TRUE),
    ('Method Misunderstanding', TRUE),
    ('Safety Concern', TRUE),
    ('Need More Practice', TRUE);
