import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'roleDisplay',
  standalone: true
})
export class RoleDisplayPipe implements PipeTransform {
  transform(role: string): string {
    const roleNames: { [key: string]: string } = {
      'PROJECT_MANAGER': 'PROJECT MANAGER',
      'QA': 'QA',
      'BA': 'BA',
      'TESTER': 'TESTER',
      'ADMIN': 'ADMIN'
    };
    return roleNames[role] || role;
  }
}
