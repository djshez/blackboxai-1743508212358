// Project Management
class ProjectManager {
    constructor() {
        this.projects = JSON.parse(localStorage.getItem('projects')) || [];
        this.activities = JSON.parse(localStorage.getItem('activities')) || [];
        this.favorites = new Set(JSON.parse(localStorage.getItem('favorites')) || []);
        this.templates = {
            projectCard: document.getElementById('projectCardTemplate'),
            activityItem: document.getElementById('activityItemTemplate')
        };
        this.setupEventListeners();
        this.renderProjects();
        this.renderActivities();
    }

    setupEventListeners() {
        // New Project Button
        document.querySelector('.bg-primary').addEventListener('click', () => {
            this.showNewProjectModal();
        });

        // Add Project Card
        document.querySelector('.border-dashed').addEventListener('click', () => {
            this.showNewProjectModal();
        });

        // Search Input
        const searchInput = document.querySelector('#searchProjects');
        searchInput.addEventListener('input', (e) => {
            const searchTerm = e.target.value;
            const status = document.querySelector('#filterStatus').value;
            this.renderProjects(searchTerm, status);
        });

        // Status Filter
        const statusFilter = document.querySelector('#filterStatus');
        statusFilter.addEventListener('change', (e) => {
            const status = e.target.value;
            const searchTerm = document.querySelector('#searchProjects').value;
            this.renderProjects(searchTerm, status);
        });

        // Sort Projects
        const sortSelect = document.querySelector('#sortProjects');
        sortSelect.addEventListener('change', (e) => {
            const sortBy = e.target.value;
            this.sortProjects(sortBy);
        });
    }

    showNewProjectModal() {
        const modal = document.createElement('div');
        modal.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50';
        modal.innerHTML = `
            <div class="bg-white rounded-lg p-6 w-full max-w-md">
                <h2 class="text-xl font-bold mb-4">Create New Project</h2>
                <form id="newProjectForm">
                    <div class="mb-4">
                        <label class="block text-gray-700 text-sm font-bold mb-2" for="projectName">
                            Project Name
                        </label>
                        <input class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" 
                            id="projectName" type="text" required>
                    </div>
                    <div class="mb-4">
                        <label class="block text-gray-700 text-sm font-bold mb-2" for="projectDescription">
                            Description
                        </label>
                        <textarea class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" 
                            id="projectDescription" rows="3"></textarea>
                    </div>
                    <div class="flex justify-end space-x-2">
                        <button type="button" class="bg-gray-300 text-gray-700 px-4 py-2 rounded hover:bg-gray-400" id="cancelProject">
                            Cancel
                        </button>
                        <button type="submit" class="bg-primary text-white px-4 py-2 rounded hover:bg-primary/90">
                            Create Project
                        </button>
                    </div>
                </form>
            </div>
        `;

        document.body.appendChild(modal);

        // Handle form submission
        document.getElementById('newProjectForm').addEventListener('submit', (e) => {
            e.preventDefault();
            const name = document.getElementById('projectName').value;
            const description = document.getElementById('projectDescription').value;
            this.createProject(name, description);
            document.body.removeChild(modal);
        });

        // Handle cancel
        document.getElementById('cancelProject').addEventListener('click', () => {
            document.body.removeChild(modal);
        });
    }

    createProject(name, description) {
        const project = {
            id: Date.now(),
            name,
            description,
            status: 'active',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        };

        this.projects.push(project);
        this.saveProjects();
        this.addActivity(`Created new project: ${name}`, 'create');
        this.renderProjects();
        this.showToast('Project created successfully', 'success');
    }

    deleteProject(id) {
        const project = this.projects.find(p => p.id === id);
        this.projects = this.projects.filter(p => p.id !== id);
        this.saveProjects();
        this.addActivity(`Deleted project: ${project.name}`, 'delete');
        this.renderProjects();
        this.showToast('Project deleted successfully', 'success');
    }

    editProject(id, name, description) {
        const project = this.projects.find(p => p.id === id);
        if (project) {
            project.name = name;
            project.description = description;
            project.updatedAt = new Date().toISOString();
            this.saveProjects();
            this.addActivity(`Updated project: ${name}`, 'update');
            this.renderProjects();
            this.showToast('Project updated successfully', 'success');
        }
    }

    addActivity(description, type = 'other') {
        const activity = {
            id: Date.now(),
            description,
            type,
            timestamp: new Date().toISOString()
        };

        this.activities.unshift(activity);
        if (this.activities.length > 10) {
            this.activities.pop();
        }
        this.saveActivities();
        this.renderActivities();
    }

    saveProjects() {
        localStorage.setItem('projects', JSON.stringify(this.projects));
    }

    saveActivities() {
        localStorage.setItem('activities', JSON.stringify(this.activities));
    }

    renderProjects(searchTerm = '', status = 'all') {
        const projectsGrid = document.querySelector('.grid');
        const addProjectCard = projectsGrid.lastElementChild;
        projectsGrid.innerHTML = '';

        const filteredProjects = this.projects.filter(project => {
            const matchesSearch = !searchTerm || 
                project.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                project.description.toLowerCase().includes(searchTerm.toLowerCase());
            
            const matchesStatus = status === 'all' || project.status === status;
            
            return matchesSearch && matchesStatus;
        });

        filteredProjects.forEach(project => {
            const card = this.createProjectCard(project);
            projectsGrid.appendChild(card);
        });

        projectsGrid.appendChild(addProjectCard);
    }

    createProjectCard(project) {
        const template = this.templates.projectCard.content.cloneNode(true);
        const card = template.querySelector('.project-card');
        
        // Set status
        const statusIndicator = card.querySelector('.status-indicator');
        const statusText = card.querySelector('.status-indicator + span');
        statusIndicator.classList.add(this.getStatusColor(project.status));
        statusText.textContent = project.status;

        // Set favorite
        const favoriteBtn = card.querySelector('.favorite-btn');
        if (this.favorites.has(project.id)) {
            favoriteBtn.classList.add('text-yellow-500');
        }
        favoriteBtn.addEventListener('click', () => this.toggleFavorite(project.id));

        // Set project details
        card.querySelector('.project-name').textContent = project.name;
        card.querySelector('.project-description').textContent = project.description;
        card.querySelector('.last-updated').textContent = this.getTimeAgo(project.updatedAt);

        // Generate and set tags
        const tags = this.generateProjectTags(project.description);
        const tagsContainer = card.querySelector('.project-tags');
        tags.forEach(tag => {
            const tagEl = document.createElement('span');
            tagEl.className = 'px-2 py-1 bg-blue-100 text-blue-800 rounded-full text-sm';
            tagEl.textContent = tag;
            tagsContainer.appendChild(tagEl);
        });

        // Add event listeners
        card.querySelector('.edit-btn').addEventListener('click', (e) => {
            e.preventDefault();
            this.showEditProjectModal(project);
        });

        card.querySelector('.archive-btn').addEventListener('click', (e) => {
            e.preventDefault();
            this.archiveProject(project.id);
        });

        card.querySelector('.delete-btn').addEventListener('click', (e) => {
            e.preventDefault();
            this.showDeleteConfirmation(project.id);
        });

        card.querySelector('.open-btn').addEventListener('click', () => {
            this.openProject(project.id);
        });

        return card;
    }

    renderActivities() {
        const container = document.getElementById('activityFeed');
        container.innerHTML = '';

        this.activities.forEach(activity => {
            const item = this.createActivityItem(activity);
            container.appendChild(item);
        });
    }

    createActivityItem(activity) {
        const template = this.templates.activityItem.content.cloneNode(true);
        const item = template.querySelector('.activity-item');
        
        const iconContainer = item.querySelector('.activity-icon-container');
        const icon = item.querySelector('.activity-icon');
        
        iconContainer.className = `w-8 h-8 rounded-full flex items-center justify-center ${
            activity.type === 'create' ? 'bg-green-100' :
            activity.type === 'update' ? 'bg-blue-100' :
            activity.type === 'delete' ? 'bg-red-100' : 'bg-gray-100'
        }`;
        
        icon.className = `fas ${this.getActivityIcon(activity.type)}`;
        
        item.querySelector('.activity-description').textContent = activity.description;
        item.querySelector('.activity-time').textContent = this.getTimeAgo(activity.timestamp);
        
        return item;
    }

    getTimeAgo(timestamp) {
        const now = new Date();
        const past = new Date(timestamp);
        const diffInSeconds = Math.floor((now - past) / 1000);

        if (diffInSeconds < 60) {
            return 'just now';
        } else if (diffInSeconds < 3600) {
            const minutes = Math.floor(diffInSeconds / 60);
            return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
        } else if (diffInSeconds < 86400) {
            const hours = Math.floor(diffInSeconds / 3600);
            return `${hours} hour${hours > 1 ? 's' : ''} ago`;
        } else {
            const days = Math.floor(diffInSeconds / 86400);
            return `${days} day${days > 1 ? 's' : ''} ago`;
        }
    }

    getStatusColor(status) {
        switch (status.toLowerCase()) {
            case 'active':
                return 'bg-green-500';
            case 'completed':
                return 'bg-blue-500';
            case 'archived':
                return 'bg-gray-500';
            default:
                return 'bg-yellow-500';
        }
    }

    getActivityIcon(type) {
        switch (type.toLowerCase()) {
            case 'create':
                return 'fa-plus-circle text-green-500';
            case 'update':
                return 'fa-edit text-blue-500';
            case 'delete':
                return 'fa-trash-alt text-red-500';
            default:
                return 'fa-info-circle text-gray-500';
        }
    }

    generateProjectTags(description) {
        const commonTags = ['web', 'mobile', 'desktop', 'frontend', 'backend', 'fullstack', 'design', 'api'];
        const tags = [];
        
        commonTags.forEach(tag => {
            if (description.toLowerCase().includes(tag)) {
                tags.push(tag);
            }
        });

        return tags.slice(0, 3); // Return max 3 tags
    }

    showToast(message, type = 'success') {
        const toast = document.createElement('div');
        toast.className = `toast ${type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`;
        toast.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'} mr-2"></i>
            <span>${message}</span>
        `;
        document.getElementById('toastContainer').appendChild(toast);
        setTimeout(() => toast.remove(), 3000);
    }

    toggleFavorite(projectId) {
        if (this.favorites.has(projectId)) {
            this.favorites.delete(projectId);
        } else {
            this.favorites.add(projectId);
        }
        localStorage.setItem('favorites', JSON.stringify([...this.favorites]));
        this.renderProjects();
    }

    archiveProject(projectId) {
        const project = this.projects.find(p => p.id === projectId);
        if (project) {
            project.status = 'archived';
            project.updatedAt = new Date().toISOString();
            this.saveProjects();
            this.addActivity(`Archived project: ${project.name}`, 'update');
            this.renderProjects();
            this.showToast('Project archived successfully');
        }
    }

    openProject(projectId) {
        const project = this.projects.find(p => p.id === projectId);
        if (project) {
            this.showToast(`Opening project: ${project.name}`);
            // Additional logic for opening project
        }
    }
}

// Initialize Project Manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    const projectManager = new ProjectManager();
});