<template>
  <div>
    <h1>Page list</h1>
    <table class="table">
      <thead>
        <tr>
          <th>Page</th>
          <th>View</th>
          <th>Edit</th>
        </tr>
      </thead>
      <tr v-for="page in pages">
        <td>{{ page }}</td>
        <td>
          <router-link :to="'/' + page">view</router-link>
        </td>
        <td>
          <router-link :to="'/' + page + '/edit'">edit</router-link>
        </td>
      </tr>
    </table>

    <ShowMore title="debug">
      <pre>
session = {{ session }}
      </pre>
    </ShowMore>
  </div>
</template>

<script>
import {ShowMore} from "@mswertz/emx2-styleguide";

export default {
  components: {
    ShowMore
  },
  props: {
    session: Object
  },
  computed: {
    pages() {
      if (this.session && this.session.settings) {
        return Object.keys(this.session.settings)
          .filter(key => key.startsWith("page."))
          .map(key => key.substring(5));
      }
    }
  }
};
</script>
