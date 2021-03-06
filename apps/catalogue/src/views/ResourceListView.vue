<template>
  <div class="bg-white">
    <div class="p-2 mb-2" :class="headerCss">
      <h6>
        <RouterLink to="/" :class="headerCss"> home</RouterLink>
        /
      </h6>
    </div>
    <TableExplorer
      :showColumns="defaultColumns"
      :showFilters="defaultFilters"
      :table="tableName"
      :showCards="defaultCards"
      @click="openDetailView"
    />
  </div>
</template>

<script>
import { TableExplorer } from "@mswertz/emx2-styleguide";

const css = {
  Institutions: "bg-info text-white",
  Datasources: "bg-warning text-dark",
  Databanks: "bg-danger text-white",
  Models: "bg-secondary text-white",
  Networks: "bg-primary text-white",
  Studies: "bg-success text-white",
  Contacts: "bg-info text-white",
  Affiliations: "bg-info text-white",
  Releases: "bg-dark text-white",
  Tables: "bg-dark text-white",
  Variables: "bg-dark text-white",
  TableMappings: "bg-dark text-white",
  VariableMappings: "bg-dark text-white",
};

export default {
  components: {
    TableExplorer,
  },
  props: {
    tableName: String,
  },
  computed: {
    headerCss() {
      return css[this.tableName];
    },
    detailRouteName() {
      //detailRoute is name of table minus trailing 's'
      return this.tableName.toLowerCase().slice(0, -1);
    },
    defaultCards() {
      if (this.tableName == "Institutions") {
        return true;
      }
      return false;
    },
    defaultColumns() {
      if (this.tableName == "Institutions") {
        return ["name", "acronym", "type", "country"];
      } else if (
        ["Datasources", "Databanks", "Networks", "Models"].includes(
          this.tableName
        )
      ) {
        return ["name", "acronym", "type", "recordPrompt", "institution"];
      } else if (this.tableName == "Studies") {
        return ["acronym", "name", "keywords"];
      } else if (this.tableName == "Contacts") {
        return [
          "name",
          "institution",
          "affiliation",
          "email",
          "orcid",
          "homepage",
        ];
      } else if (this.tableName == "Affiliations") {
        return ["name", "homepage", "acronym"];
      } else if (this.tableName == "Tables") {
        return [
          "release",
          "name",
          "label",
          "unitOfObservation",
          "topics",
          "description",
        ];
      } else if (this.tableName == "Variables") {
        return [
          "release",
          "table",
          "name",
          "label",
          "format",
          "unit",
          "topics",
          "description",
          "mandatory",
        ];
      }
      return [];
    },
    defaultFilters() {
      if (this.tableName == "Institutions") {
        return ["type", "country"];
      }
      if (this.tableName == "Studies") {
        return ["keywords", "networks", "startYear", "endYear"];
      }
      if (this.tableName == "Databanks") {
        return ["keywords", "recordPrompt"];
      }
      return [];
    },
  },
  methods: {
    openDetailView(row) {
      // in case of table
      if (this.tableName == "Tables") {
        this.$router.push({
          name: this.detailRouteName,
          params: {
            acronym: row.release.resource.acronym,
            version: row.release.version,
            name: row.name,
          },
        });
      } else if (
        this.tableName == "TableMappings" ||
        this.tableName == "VariableMappings"
      ) {
        this.$router.push({
          name: "tablemapping",
          params: {
            fromAcronym: row.fromRelease.resource.acronym,
            fromVersion: row.fromRelease.version,
            fromTable: row.fromTable.name,
            toAcronym: row.toRelease.resource.acronym,
            toVersion: row.toRelease.version,
            toTable: row.toTable.name,
          },
        });
      } else if (this.tableName == "Variables") {
        this.$router.push({
          name: this.detailRouteName,
          params: {
            acronym: row.release.resource.acronym,
            version: row.release.version,
            table: row.table.name,
            name: row.name,
          },
        });
      } else if (row.version) {
        this.$router.push({
          name: this.detailRouteName,
          params: { acronym: row.resource.acronym, version: row.version },
        });
      } else if (row.acronym) {
        this.$router.push({
          name: this.detailRouteName,
          params: { acronym: row.acronym },
        });
      } else {
        this.$router.push({
          name: this.detailRouteName,
          params: { name: row.name },
        });
      }
    },
  },
};
</script>
