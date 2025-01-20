  import Controller from '@ember/controller';
  import { action } from '@ember/object';
  import { tracked } from '@glimmer/tracking';
  // import Chart from 'chart.js/auto';


  export default class GroupController extends Controller {
    @tracked groups = [];
    @tracked selectedGroup = null;
    @tracked selectedLastGroup = null;
    // @tracked selectedLogGroup = null;
    @tracked sortBy = '';
    @tracked searchQuery = '';
    @tracked totalCount = 0;
    @tracked newGroupName = '';
    @tracked newGroupDescription = '';
    @tracked newGroupMail = '';
    @tracked isNewGroupPopupVisible = false;
    @tracked isAddUserPopupVisible = false;
    @tracked isReportPopupVisible = false;
    @tracked isGroupDetailsPopupVisible = false;
    @tracked userName = '';
    @tracked groupName = '';
    @tracked addUserError = '';
    @tracked addUserSuccess = '';
    @tracked createGroupError = '';
    @tracked groupCreationData = {};
    @tracked groupDetails = [];
    @tracked isLogDetailsPopupVisible = false;
    @tracked selectedLogGroup = [];

    @tracked isRecoverPopupVisible = false;
    @tracked recoverAccountName = '';
    @tracked recoverTimeCreated = '';
    @tracked recoverUserError = '';

    constructor() {
      super(...arguments);
      this.fetchGroups();
    }

    @action
    async fetchGroups(params = {}) {
      params.sortBy = this.sortBy;
      params.search = this.searchQuery;
      const query = new URLSearchParams(params).toString();
      const url = `http://localhost:8080/backend_war_exploded/GroupServlet?${query}`;

      try {
        const response = await fetch(url);
        if (!response.ok) {
          throw new Error(`Failed to fetch groups: ${response.statusText}`);
        }
        const data = await response.json();
        this.groups = data.groups || [];
        this.totalCount = data.totalCount || 0;
      } catch (error) {
        console.error('Error fetching groups:', error);
        this.groups = [];
        this.totalCount = 0;
      }
    }

    @action
    async fetchGroupCreationData() {
      const url = `http://localhost:8080/backend_war_exploded/GroupCreationReportServlet`;

      try {
        const response = await fetch(url);
        if (!response.ok) {
          throw new Error(`Failed to fetch group creation data: ${response.statusText}`);
        }
        const data = await response.json();
        this.groupCreationData = data.data;
        this.displayGroupReportChart();
      } catch (error) {
        console.error('Error fetching group creation data:', error);
      }
    }


    @action
    openRecoverPopup() {
      this.isRecoverPopupVisible = true;
    }
  
    @action
    closeRecoverPopup() {
      this.isRecoverPopupVisible = false;
    }
  
    @action
    updateRecoverAccountName(event) {
      this.recoverAccountName = event.target.value;
    }
  
    @action
    updateRecoverTimeCreated(event) {
      this.recoverTimeCreated = event.target.value;
    }
  
    @action
    async recoverUser(event) {
      console.log('Recover user:', this.recoverAccountName, this.recoverTimeCreated);
      event.preventDefault();
      if (!this.recoverAccountName || !this.recoverTimeCreated) {
        alert('All fields are required!');
        return;
      }
      try {
        const recoverData = new URLSearchParams();
        recoverData.append('recoverAccountName', this.recoverAccountName);
        recoverData.append('recoverTimeCreated', this.recoverTimeCreated);
    
        const response = await fetch(
          'http://localhost:8080/backend_war_exploded/RecoverGroupServlet',
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded',
              'Authorization': 'Bearer token', 
            },
            body: recoverData,
          },
        );
        console.log('Recover user response:', response);
        const result = await response.json();
        console.log('Recover user response:', result);
        if (result.status === 'success') {
          this.fetchUsers();
          this.closeRecoverPopup();
        } else {
          // this.recoverUserError = 'Failed to recover user!';
        }
      } catch (error) {
        console.error('Error:', error);
        // this.recoverUserError = 'Failed to recover user!';
      }
    }
  

    @action
    openReportPopup() {
      this.isReportPopupVisible = true;
      this.fetchGroupCreationData();
    }

    @action
    closeReportPopup() {
      this.isReportPopupVisible = false;
    }

    @action
    async showGroupsForDay(day) {
      console.log('Fetching groups for the day:', day);
      try {
        const response = await fetch(
          `http://localhost:8080/backend_war_exploded/FetchGroupNamesForDayServlet?day=${day}`,
        );
        if (!response.ok) {
          throw new Error(
            `Failed to fetch groups for the day: ${response.statusText}`,
          );
        }
        const groupDetails = await response.json();
        this.groupDetails = groupDetails.groups;
        this.isGroupDetailsPopupVisible = true;
      } catch (error) {
        console.error('Error fetching groups for the day:', error);
      }
    }

    @action
    closeGroupDetailsPopup() {
      this.isGroupDetailsPopupVisible = false;
      this.groupDetails = [];
    }

    @action
    async showGroupDetails(groupName) {
      try {
        const response = await fetch(`http://localhost:8080/backend_war_exploded/FetchGroupData?groupName=${groupName}`);
        if (!response.ok) {
          throw new Error(`Failed to fetch group details: ${response.statusText}`);
        }
        const group = await response.json();
        // console.log(group);
        this.selectedGroup = {
          name: group.name,
          description: group.description || 'No Description Found',
          mail: group.mail || 'No Mail Found',
          whenCreated : group.whenCreated || '',
          whenChanged : group.whenChanged || '',
        };
      } catch (error) {
        console.error('Error fetching group details:', error);
      }
      console.log('hi',this.selectedGroup); 
    }

    @action
    async showLastModDetails(groupName) {
      console.log('Fetching last modified details for group----------:', groupName);
      try {
        const response = await fetch(`http://localhost:8080/backend_war_exploded/FetchLastModUsr?objName=${groupName}`);
        if (!response.ok) {
          throw new Error(`Failed to fetch last modified details: ${response.statusText}`);
        }
        const data = await response.json();
      } catch (error) {
        console.error('Error fetching last modified details:', error);
      }
    }


    @action
    async showLogDetails(groupName) {
      // this.showLastModDetails(groupName);
      this.userName = groupName;
      try {
        const response = await fetch(`http://localhost:8080/backend_war_exploded/FetchUserLog?accountName=${groupName}`);
        if (!response.ok) {
          throw new Error(`Failed to fetch log details: ${response.statusText}`);
        }
        const data = await response.json();
        console.log('Fetched log details:', data);
        this.selectedLogGroup = data.map(log => ({
          Message: log.Message || 'No Message Found',
          TimeCreated: log.TimeCreated || 'No Time Found',
          OldValue: log.OldValue || 'No Old Value Found',
          NewValue: log.NewValue || 'No New Value Found',
          AccountName: log.AccountName || 'No Account Name Found',
          ChangedOn: log.ChangedOn || 'No Changed On Found',
        }));
        this.isLogDetailsPopupVisible = true;
      } catch (error) {
        console.error('Error fetching log details:', error);
        this.selectedLogGroup = [{ Message: 'Error fetching data', TimeCreated: '', OldValue: '', NewValue: '' }];
      }
    }

    @action
    closePopup() {
      this.selectedGroup = null;
      this.selectedLastGroup = null;
      this.selectedLogGroup = null;
      this.isLogDetailsPopupVisible = false;

    }

    displayGroupReportChart(){
      const ctx = document.getElementById('groupReportChart').getContext('2d');
      const labels = Object.keys(this.groupCreationData);
      const data = Object.values(this.groupCreationData).map(item => item.count);

      new Chart(ctx,{
        type: 'line',
        data: {
          labels: labels,
          datasets: [{
            label: 'Groups Created',
            data: data,
            borderColor: 'rgba(75, 192, 192, 1)',
            borderWidth: 2,
            fill: false
          }]
        },
        options: {
          scales: {
            x: {
              title: {
                display: true,
                text: 'Days'
              }
            },
            y: {
              title: {
                display: true,
                text: 'Count'
              }
            }
          },
          onClick: (event, elements) => {
            console.log('Clicked on:', elements);
            console.log('Labels:', labels);
            console.log('Data:', data);
            if(elements.length > 0){
              const index = elements[0]._index;
              console.log('Index:', index);
              const day = labels[index];
              console.log('Day:', day);
              this.showGroupsForDay(day);
            }
          },
          tooltips: {
            callbacks: {
              label: (tooltipItem) => {
                const day = labels[tooltipItem.index];
                const count = data[tooltipItem.index];
                return `Date: ${day}\nCount: ${count}`;
              }
            }
          }
        }
      });
    }
    @action
    updateSortBy(event) {
      this.sortBy = event.target.value;
      this.fetchGroups();
    }

    @action
    updateSearchQuery(event) {
      this.searchQuery = event.target.value;
      this.fetchGroups();
    }

    @action
    openNewGroupPopup() {
      this.isNewGroupPopupVisible = true;
    }
    @action
    closeNewGroupPopup() {
      this.isNewGroupPopupVisible = false;
      this.newGroupName = '';
      this.newGroupDescription = '';
      this.newGroupMail = '';
      this.createGroupError = '';
    }
    @action
    updateNewGroupName(event) {
      this.newGroupName = event.target.value;
    }

    @action
    updateNewGroupDescription(event) {
      this.newGroupDescription = event.target.value;
    }

    @action
    updateNewGroupMail(event) {
      this.newGroupMail = event.target.value;
    }

    @action
    async createGroup(event) {
      event.preventDefault();

      if (!this.newGroupName || !this.newGroupDescription || !this.newGroupMail) {
        alert('All fields are required!');
        return;
      }

      try {
        const response = await fetch(
          'http://localhost:8080/backend_war_exploded/CreateGroupServlet',
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              groupName: this.newGroupName,
              description: this.newGroupDescription,
              mail: this.newGroupMail,
            }),
          },
        );

        const result = await response.json();
        if (result.status === 'success') {
          this.fetchGroups();
          this.closeNewGroupPopup();
        } else {
          this.createGroupError = result.message || 'Failed to create group!';
        }
      } catch (error) {
        console.error('Error:', error);
        this.createGroupError = 'Failed to create group!';
      }
    }

    @action
    openAddUserPopup() {
      this.isAddUserPopupVisible = true;
    }

    @action
    closeAddUserPopup() {
      this.isAddUserPopupVisible = false;
      this.userName = '';
      this.groupName = '';
      this.addUserError = '';
      this.addUserSuccess = '';
    }

    @action
    updateUserName(event) {
      this.userName = event.target.value;
    }

    @action
    updateGroupName(event) {
      this.groupName = event.target.value;
    }

    @action
    async addUserToGroup(event) {
      event.preventDefault();

      if (!this.userName || !this.groupName) {
        alert('All fields are required!');
        return;
      }

      try {
        const response = await fetch(
          'http://localhost:8080/backend_war_exploded/AddUserToGroupServlet',
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              userName: this.userName,
              groupName: this.groupName,
            }),
          },
        );

        const result = await response.json();
        if (result.status === 'success') {
          this.addUserSuccess = 'User added to group successfully!';
          this.addUserError = '';
        } else if (result.message.includes('User already exists in the group')) {
          this.addUserError = 'User already exists in the group!';
          this.addUserSuccess = '';
        } else if (result.message.includes('User not found in AD')) {
          this.addUserError = 'User not found or mismatch!';
          this.addUserSuccess = '';
        } else {
          this.addUserError = 'Invalid user or group!';
          this.addUserSuccess = '';
        }
      } catch (error) {
        console.error('Error:', error);
        this.addUserError = 'Invalid user or group!';
        this.addUserSuccess = '';
      }
    }

    @action
    confirmDelete(groupName) {
      if (confirm(`Are you sure you want to delete the group '${groupName}'?`)) {
        this.deleteGroup(groupName);
      }
    }

    @action
    async deleteGroup(groupName) {
      try {
        const response = await fetch(
          'http://localhost:8080/backend_war_exploded/DeleteGroupServlet',
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              groupName: groupName,
            }),
          },
        );

        const result = await response.json();
        if (result.status === 'success') {
          this.fetchGroups();
        } else {
          alert(result.message || 'Failed to delete group!');
        }
      } catch (error) {
        console.error('Error:', error);
        alert('Failed to delete group!');
      }
    }
  }

