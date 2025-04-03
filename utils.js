// Utility functions for the workspace
class Utils {
    static showToast(message, type = 'success') {
        const toast = document.createElement('div');
        toast.className = `toast ${type === 'success' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`;
        toast.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'} mr-2"></i>
            <span>${message}</span>
        `;
        document.getElementById('toastContainer').appendChild(toast);
        setTimeout(() => toast.remove(), 3000);
    }

    static showLoading(show = true) {
        const overlay = document.getElementById('loadingOverlay');
        overlay.style.display = show ? 'flex' : 'none';
    }

    static formatDate(date) {
        const now = new Date();
        const diff = now - new Date(date);
        const seconds = Math.floor(diff / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (days > 0) {
            return `${days} day${days > 1 ? 's' : ''} ago`;
        } else if (hours > 0) {
            return `${hours} hour${hours > 1 ? 's' : ''} ago`;
        } else if (minutes > 0) {
            return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
        } else {
            return 'just now';
        }
    }

    static getStatusColor(status) {
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

    static getActivityIcon(type) {
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

    static debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    static validateProject(name, description) {
        const errors = [];
        if (!name.trim()) {
            errors.push('Project name is required');
        }
        if (!description.trim()) {
            errors.push('Project description is required');
        }
        if (name.length > 50) {
            errors.push('Project name must be less than 50 characters');
        }
        if (description.length > 500) {
            errors.push('Project description must be less than 500 characters');
        }
        return errors;
    }

    static generateProjectTags(description) {
        const commonTags = ['web', 'mobile', 'desktop', 'frontend', 'backend', 'fullstack', 'design', 'api'];
        const tags = [];
        
        commonTags.forEach(tag => {
            if (description.toLowerCase().includes(tag)) {
                tags.push(tag);
            }
        });

        return tags.slice(0, 3); // Return max 3 tags
    }
}

export default Utils;