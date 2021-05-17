import Vue from "vue";

export default {
  setVariables(state, variables) {
    Vue.set(state, "variables", Array.isArray(variables) ? variables : []);
  },
  setVariableCount(state, variableCount) {
    state.variableCount = variableCount;
  },
  setVariableDetails(state, { variableName, variableDetails }) {
    Vue.set(state.variableDetails, variableName, variableDetails);
  },
  setSearchInput(state, searchInput) {
    state.searchInput = searchInput;
  },
  setKeywords(state, keywords) {
    state.keywords = keywords;
  },
  setCohorts(state, cohorts) {
    Vue.set(state, "cohorts", cohorts);
  },
  setVariableMappings(state, variableMappings) {
    Vue.set(state, "variableMappings", variableMappings);
  },
  removeKeywordFromSelection(state, keywordName) {
    state.selectedKeywords = state.selectedKeywords.filter(
      (sk) => sk !== keywordName
    );
  },
};