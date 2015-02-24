/**
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.bitbucket.api.impl;

import static java.util.Arrays.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.social.bitbucket.api.BitBucketChangeset;
import org.springframework.social.bitbucket.api.BitBucketChangesets;
import org.springframework.social.bitbucket.api.BitBucketDeployKey;
import org.springframework.social.bitbucket.api.BitBucketDirectory;
import org.springframework.social.bitbucket.api.BitBucketFile;
import org.springframework.social.bitbucket.api.BitBucketRepository;
import org.springframework.social.bitbucket.api.BitBucketService;
import org.springframework.social.bitbucket.api.BitBucketUser;
import org.springframework.social.bitbucket.api.RepoCreation;
import org.springframework.social.bitbucket.api.RepoOperations;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RepoTemplate extends AbstractBitBucketOperations implements
        RepoOperations {

    public RepoTemplate(final RestTemplate restTemplate,
            final boolean authorized) {
        super(restTemplate, authorized);
    }

    @Override
    public BitBucketRepository getRepository(final String user,
            final String repoSlug) {
        return restTemplate.getForObject(
                buildUrl("/repositories/{user}/{slug}/").toString(),
                BitBucketRepository.class, user, repoSlug);
    }

    @Override
    public List<BitBucketRepository> getUserRepositories() {
        return asList(restTemplate.getForObject(
                buildUrl("/user/repositories/"), BitBucketRepository[].class));
    }

    @Override
    public List<BitBucketRepository> search(final String query) {
        return restTemplate.getForObject(buildUrl("/repositories/?name={q}"),
                SearchResultHolder.class, query).repositories;
    }

    @Override
    public Map<String, BitBucketChangeset> getTags(final String user,
            final String repoSlug) {
        return restTemplate.getForObject(
                buildUrl("/repositories/{user}/{slug}/tags/").toString(),
                Tags.class, user, repoSlug);

    }

    @Override
    public List<BitBucketUser> getFollowers(final String user,
            final String repoSlug) {
        return restTemplate.getForObject(
                buildUrl("/repositories/{user}/{slug}/followers/").toString(),
                        FollowersHolder.class, user, repoSlug).followers;
    }

    @Override
    public BitBucketChangesets getChangesets(final String user,
            final String repoSlug) {
        return restTemplate.getForObject(
                buildUrl("/repositories/{user}/{slug}/changesets/").toString(),
                BitBucketChangesets.class, user, repoSlug);
    }

    @Override
    public BitBucketChangesets getChangesets(final String user,
            final String repoSlug, final String start, final int limit) {
        return restTemplate
                .getForObject(
                        buildUrl(
                                "/repositories/{user}/{slug}/changesets/?start={start}&limit={limit}")
                                .toString(), BitBucketChangesets.class, user,
                        repoSlug, start, limit);
    }

    @Override
    public BitBucketDirectory getDirectory(final String user,
            final String repoSlug, final String revision, final String path) {
        return restTemplate.getForObject(
                buildUrl("/repositories/{user}/{slug}/src/{rev}/{path}/")
                        .toString(), BitBucketDirectory.class, user, repoSlug,
                revision, path);
    }

    @Override
    public BitBucketFile getFile(final String user, final String repoSlug,
            final String revision, final String path) {
        return restTemplate.getForObject(
                buildUrl("/repositories/{user}/{slug}/src/{rev}/{path}")
                        .toString(), BitBucketFile.class, user, repoSlug,
                revision, path);
    }

    @Override
    public BitBucketRepository createRepository(final RepoCreation options) {
        return restTemplate.postForObject(buildUrl("/repositories"), options,
                BitBucketRepository.class);
    }

    @Override
    public List<BitBucketDeployKey> addDeployKey(final String user,
            final String repoSlug, final String keyContent, final String label) {

        // post params
        MultiValueMap<String, Object> postParams = new LinkedMultiValueMap<String, Object>();
        postParams.add("key", keyContent);
        postParams.add("label", label);

        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<MultiValueMap<String, Object>>(
                postParams);

        ResponseEntity<BitBucketDeployKeyHolder> entity = restTemplate
                .exchange(buildUrl("/repositories/{user}/{slug}/deploy-keys")
                        .toString(), HttpMethod.POST, httpEntity,
                        BitBucketDeployKeyHolder.class, user, repoSlug);

        return entity.getBody().deployKeys;
    }

    @Override
    public List<BitBucketService> addService(final String user,
            final String repoSlug, final String hookUrl) {

        // post params
        MultiValueMap<String, Object> postParams = new LinkedMultiValueMap<String, Object>();
        postParams.add("type", "POST");
        postParams.add("URL", hookUrl);

        return restTemplate.postForObject(
                buildUrl("/repositories/{user}/{slug}/services").toString(),
                postParams, ServicesHolder.class, user, repoSlug).services;
    }

    /**
     * Exists for the sole purpose of having a strongly typed Map for Jackson.
     */
    private static class Tags extends HashMap<String, BitBucketChangeset> {
        private static final long serialVersionUID = 1L;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FollowersHolder {

        @JsonProperty
        private List<BitBucketUser> followers;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SearchResultHolder {

        @JsonProperty
        private List<BitBucketRepository> repositories;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DeployKeysHolder {

        @JsonProperty
        private List<BitBucketDeployKey> deployKeys;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ServicesHolder {

        @JsonProperty
        private List<BitBucketService> services;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BitBucketDeployKeyHolder {

        @JsonProperty
        private List<BitBucketDeployKey> deployKeys;
    }

    private static final ParameterizedTypeReference<List<BitBucketDeployKey>> responseType = new ParameterizedTypeReference<List<BitBucketDeployKey>>() {
    };
}
