-- Drop web_data_id from etfs (added in V008)
--
-- The column only ever served the iShares web importer, which built its AJAX URL as
-- {web_url}/{web_data_id}.ajax. iShares retired that endpoint (it answers HTTP 404), and the
-- importer now reads the required parameters from the product page itself, so web_url alone is
-- enough. No importer reads this column any more.
ALTER TABLE etfs DROP COLUMN web_data_id;
