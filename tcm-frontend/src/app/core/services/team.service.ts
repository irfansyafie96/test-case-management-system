import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TeamService {
  private apiUrl = environment.apiUrl || 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  /**
   * Invite a new member to the organization
   * @param email Email address of the invitee
   * @param role Role to assign (QA, BA, TESTER)
   */
  inviteMember(email: string, role: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/invitations`, { email, role }, { responseType: 'text' });
  }

  /**
   * Accept an invitation and create account
   * @param token Invitation token
   * @param username Desired username
   * @param password Desired password
   */
  acceptInvitation(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/invitations/accept`, data, { responseType: 'text' });
  }

  /**
   * Get invitation details (to show on Join page)
   * @param token Invitation token
   */
  getInvitation(token: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/invitations/${token}`);
  }
}
