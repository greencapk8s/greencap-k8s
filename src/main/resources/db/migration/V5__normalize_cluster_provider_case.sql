ALTER TABLE clusters DROP CONSTRAINT clusters_provider_check;

UPDATE clusters SET provider = 'Kubernetes' WHERE provider = 'KUBERNETES';
UPDATE clusters SET provider = 'OpenShift'  WHERE provider = 'OPENSHIFT';

ALTER TABLE clusters ADD CONSTRAINT clusters_provider_check
    CHECK (provider IN ('Kubernetes', 'OpenShift'));
