<template>
  <div>
    <MessageError v-if="error">{{ error }}</MessageError>
    <h1>
      <small>institution</small><br />{{ institution.name }} ({{
        institution.acronym
      }})
    </h1>
    <label>Website:</label>
    <a :href="institution.website">{{ institution.website }}</a> <br />
    <label>Description:</label>
    <p>{{ institution.description }}</p>
    <h4>Datasources:</h4>
    <DatasourceList
      :institutionAcronym="institutionAcronym"
      :showSearch="false"
    />
    <h4>Databanks:</h4>
    <DatabankList
      :institutionAcronym="institutionAcronym"
      :showSearch="false"
    />
    <h4>Networks:</h4>
    <ProjectList
      :filter="{
        provider: {
          acronym: { equals: this.institutionAcronym },
        },
      }"
    />
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError, ReadMore } from "@mswertz/emx2-styleguide";
import DatabankList from "../components/DatabankList";
import ProjectList from "../components/ProjectList";
import DatasourceList from "../components/DatasourceList";

export default {
  components: {
    DatasourceList,
    ProjectList,
    DatabankList,
    MessageError,
    ReadMore,
  },
  props: {
    institutionAcronym: String,
  },
  data() {
    return {
      error: null,
      institution: {},
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Institutions($acronym:String){Institutions(filter:{acronym:{equals:[$acronym]}}){name,acronym,description,website,resources{acronym,name}}}`,
        {
          acronym: this.institutionAcronym,
        }
      )
        .then((data) => {
          this.institution = data.Institutions[0];
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
  watch: {
    institutionAcronym() {
      this.reload();
    },
  },
};
</script>