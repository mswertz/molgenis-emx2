<template>
  <div class="container-fluid pt-3">
    <ul v-if="variable" class="nav nav-tabs">
      <li
        class="nav-item"
        v-for="mapping in variable.mappings"
        :key="mapping.fromTable.release.resource.acronym"
      >
        <router-link
          class="nav-link"
          :to="{
            name: 'resourceHarmonizationDetails',
            params: {
              name,
              version,
              network,
              sourceCohort: mapping.fromTable.release.resource.acronym,
            },
          }"
        >
          {{ mapping.fromTable.release.resource.acronym }}
        </router-link>
      </li>
    </ul>
    <router-view :key="$route.fullPath" :variable="variable"></router-view>
  </div>
</template>

<script>
export default {
  name: "SingleVarHarmonizationView",
  props: {
    name: String,
    network: String,
    version: String,
    variable: Object,
  },
  async created() {
    // initialy select the first mapping
    if (
      this.variable.mappings &&
      this.variable.mappings[0] &&
      !this.$route.params.acronym
    ) {
      this.$router.replace({
        name: "resourceHarmonizationDetails",
        params: {
          name: this.name,
          network: this.network,
          version: this.version,
          sourceCohort:
            this.variable.mappings[0].fromTable.release.resource.acronym,
        },
      });
    }
  },
};
</script>

<style></style>
