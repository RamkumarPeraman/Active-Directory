import Controller from '@ember/controller';
import { action } from '@ember/object';
import { tracked } from '@glimmer/tracking';
import Chart from 'chart.js/auto';
import { log } from 'qunit';

export default class UsersController extends Controller {
  @tracked users = [];
  @tracked selectedUser = null;
  @tracked selectedLogGroup = [];
  @tracked sortBy = '';
  @tracked searchQuery = '';
  @tracked totalCount = 0;
  @tracked firstName = '';
  @tracked lastName = '';
  @tracked displayName = '';
  @tracked mail = '';
  @tracked logOnName = '';
  @tracked description = '';
  @tracked telephoneNumber = '';
  @tracked isNewUserPopupVisible = false;
  @tracked createUserError = '';
  @tracked isReportPopupVisible = false;
  @tracked userCreationData = {};
  @tracked userDetails = [];
  @tracked isUserDetailsPopupVisible = false;
  @tracked isLogDetailsPopupVisible = false;
  @tracked userName = '';
  @tracked accountName = '';
  @tracked timeCreated = '';
  @tracked isRecoverPopupVisible = false;
  @tracked recoverAccountName = '';
  @tracked recoverTimeCreated = '';
  @tracked recoverUserError = '';

  constructor() {
    super(...arguments);
    this.fetchUsers();
  }

  @action
  async fetchUsers() {
    const params = {
      sortBy: this.sortBy,
      search: this.searchQuery,
    };
    console.log('Fetching users with params:', params);
    const query = new URLSearchParams(params).toString();
    const url = `http://localhost:8080/backend_war_exploded/UserServlet?${query}`;

    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Failed to fetch users: ${response.statusText}`);
      }
      const data = await response.json();
      console.log('Fetched user data:', data);
      this.users = data;
      this.totalCount = data.length;
    } catch (error) {
      console.error('Error fetching users:', error);
      this.users = [];
      this.totalCount = 0;
    }
  }

  @action
  updateSortBy(event) {
    this.sortBy = event.target.value;
    console.log('Updated sortBy:', this.sortBy);
    this.fetchUsers();
  }

  @action
  updateSearchQuery(event) {
    this.searchQuery = event.target.value;
    console.log('Updated searchQuery:', this.searchQuery);
    this.fetchUsers();
  }

  @action
  async showUserDetails(displayName) {
    try {
      const response = await fetch(`http://localhost:8080/backend_war_exploded/FetchUserData?displayName=${displayName}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch user details: ${response.statusText}`);
      }
      const user = await response.json();
      console.log('Fetched user details:', user);
      this.selectedUser = {
        name: user.name,
        description: user.description || 'No Description Found',
        mail: user.mail || 'No Mail Found',
        logOnName: user.logOnName || 'No LogOn Name Found',
        telephoneNumber: user.telephoneNumber || 'No Telephone Number Found',
        address: user.address || 'No Address Found',
        whenCreated: user.whenCreated || '',
        whenChanged: user.whenChanged || '',
      };
    } catch (error) {
      console.error('Error fetching user details:', error);
    }
  }

  @action
  async showLastModDetails(displayName) {
    console.log('Fetching last modified details for group----------:', displayName);
    try {
      const response = await fetch(`http://localhost:8080/backend_war_exploded/FetchLastModUsr?objName=${displayName}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch last modified details: ${response.statusText}`);
      }
      const data = await response.json();
      console.log('Fetched last modified details:', data);
    } catch (error) {
      console.error('Error fetching last modified details:', error);
    }
  }

  @action
  async showLogDetails(displayName) {
    this.showLastModDetails(displayName);
    this.userName = displayName;
    try {
      const response = await fetch(`http://localhost:8080/backend_war_exploded/FetchUserLog?accountName=${displayName}`);
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
    this.selectedUser = null;
    this.selectedLogGroup = [];
    this.isUserDetailsPopupVisible = false;
    this.isLogDetailsPopupVisible = false;
  }

  @action
  openNewUserPopup() {
    this.isNewUserPopupVisible = true;
  }

  @action
  closeNewUserPopup() {
    this.isNewUserPopupVisible = false;
    this.firstName = '';
    this.lastName = '';
    this.displayName = '';
    this.logOnName = '';
    this.mail = '';
    this.description = '';
    this.telephoneNumber = '';
    this.createUserError = '';
    this.accountName = '';
    this.timeCreated = '';
  }

  @action
  updateFirstName(event) {
    this.firstName = event.target.value;
  }

  @action
  updateLastName(event) {
    this.lastName = event.target.value;
  }

  @action
  updateDisplayName(event) {
    this.displayName = event.target.value;
  }

  @action
  updateLogOnName(event) {
    this.logOnName = event.target.value;
  }

  @action
  updateMail(event) {
    this.mail = event.target.value;
  }

  @action
  updateDescription(event) {
    this.description = event.target.value;
  }

  @action
  updateTelephoneNumber(event) {
    this.telephoneNumber = event.target.value;
  }

  @action
  updateAccountName(event) {
    this.accountName = event.target.value;
  }

  @action
  updateTimeCreated(event) {
    this.timeCreated = event.target.value;
  }

  @action
  async createUser(event) {
    event.preventDefault();
    if (!this.firstName || !this.lastName || !this.displayName || !this.logOnName || !this.mail || !this.description || !this.telephoneNumber || !this.accountName || !this.timeCreated) {
      alert('All fields are required!');
      return;
    }
    try {
      const response = await fetch(
        'http://localhost:8080/backend_war_exploded/CreateUserServlet',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            firstName: this.firstName,
            lastName: this.lastName,
            mail: this.mail,
            logOnName: this.logOnName,
            phnnumber: this.telephoneNumber,
            description: this.description,
            displayname: this.displayName,
            accountName: this.accountName,
            timeCreated: this.timeCreated,
          }),
        },
      );
      const result = await response.json();
      console.log('Create user response:', result);
      if (result.status === 'success') {
        this.fetchUsers();
        this.closeNewUserPopup();
      } else if (result.message.includes('User already exists')) {
        this.createUserError = 'User already exists!';
      } else {
        this.createUserError = 'Failed to create user!';
      }
    } catch (error) {
      console.error('Error:', error);
      this.createUserError = 'Failed to create user!';
    }
  }

  @action
  confirmDelete(displayName) {
    if (confirm(`Are you sure you want to delete the user '${displayName}'?`)) {
      this.deleteUser(displayName);
    }
  }

  @action
  async deleteUser(displayName) {
    try {
      const response = await fetch(
        'http://localhost:8080/backend_war_exploded/DeleteUserServlet',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ displayName }),
        },
      );
      const result = await response.json();
      console.log('Delete user response:', result);
      if (result.status === 'success') {
        this.fetchUsers();
      } else {
        alert(result.message || 'Failed to delete user!');
      }
    } catch (error) {
      console.error('Error:', error);
      alert('Failed to delete user!');
    }
  }

  @action
  openReportPopup() {
    this.isReportPopupVisible = true;
    this.fetchUserCreationData();
  }

  @action
  closeReportPopup() {
    this.isReportPopupVisible = false;
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
        'http://localhost:8080/backend_war_exploded/RecoverUserServlet',
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
  async fetchUserCreationData() {
    const url = `http://localhost:8080/backend_war_exploded/UserCreationReportServlet`;

    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Failed to fetch user creation data: ${response.statusText}`);
      }
      const data = await response.json();
      console.log('Fetched user creation data:', data);
      this.userCreationData = data.data;
      this.displayUserReportChart();
    } catch (error) {
      console.error('Error fetching user creation data:', error);
    }
  }

  displayUserReportChart() {
    const ctx = document.getElementById('userReportChart').getContext('2d');
    const labels = Object.keys(this.userCreationData);
    const data = Object.values(this.userCreationData).map(item => item.count);

    new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Users Created',
          data: data,
          borderColor: 'rgba(75, 192, 192, 1)',
          borderWidth: 2,
          fill: false,
        }],
      },
      options:{
        scales: {
          x: {
            title: {
              display: true,
              text: 'Days',
            },
          },
          y:{
            title:{
              display: true,
              text: 'Count',
            },
          },
        },
        onClick:(event, elements) => {
          if(elements.length > 0) {
            const index = elements[0].index;
            const day = labels[index];
            this.showUsersForDay(day);
          }
        },
        plugins:{
          tooltip:{
            callbacks:{
              label:(tooltipItem) => {
                const day = labels[tooltipItem.dataIndex];
                const count = data[tooltipItem.dataIndex];
                return `Date: ${day}\nCount: ${count}`;
              },
            },
          },
        },
      },
    });
  }
  @action
  async showUsersForDay(day) {
    try {
      const response = await fetch(
        `http://localhost:8080/backend_war_exploded/FetchUserNamesForDayServlet?day=${day}`,
      );
      if (!response.ok) {
        throw new Error(
          `Failed to fetch users for the day: ${response.statusText}`,
        );
      }
      const userDetails = await response.json();
      console.log('Fetched users for the day:', userDetails);
      this.userDetails = userDetails.Users;
      this.isUserDetailsPopupVisible = true;
    } catch (error) {
      console.error('Error fetching users for the day:', error);
    }
  }
  @action
  closeUserDetailsPopup() {
    this.isUserDetailsPopupVisible = false;
    this.userDetails = [];
  }
}