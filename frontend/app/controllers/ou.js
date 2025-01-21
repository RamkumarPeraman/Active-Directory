import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';

export default class OUController extends Controller {
  @tracked ous = [];
  @tracked selectedOU = null;

  @action
  async fetchOUs(params = {}) {
    const query = new URLSearchParams(params).toString();
    const url = `http://localhost:8080/backend_war_exploded/OUServlet?${query}`;

    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(
          `Failed to fetch organizational units: ${response.statusText}`,
        );
      }
      this.ous = await response.json();
    } catch (error) {
      console.error('Error fetching organizational units:', error);
      this.ous = [];
    }
  }

  @action
  async showOUDetails(ouId) {
    try {
      const response = await fetch(
        `http://localhost:8080/backend_war_exploded/OUServlet?id=${ouId}`,
      );
      if (!response.ok) {
        throw new Error(
          `Failed to fetch organizational unit details: ${response.statusText}`,
        );
      }
      this.selectedOU = await response.json();
    } catch (error) {
      console.error('Error fetching organizational unit details:', error);
      this.selectedOU = null;
    }
  }

  @action
  closePopup() {
    this.selectedOU = null;
  }
}
