import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';

export interface AnalyticsData {
  totalTestCases: number;
  executedCount: number;
  passedCount: number;
  failedCount: number;
  notExecutedCount: number;
  passRate: number;
  failRate: number;
  byModule: ModuleAnalytics[];
}

export interface ModuleAnalytics {
  moduleId: number;
  moduleName: string;
  projectId: number;
  projectName: string;
  totalTestCases: number;
  executedCount: number;
  passedCount: number;
  failedCount: number;
  notExecutedCount: number;
}

export interface FilterContext {
  user?: string;
  project?: string;
  module?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PdfExportService {

  constructor() { }

  exportAnalyticsReport(data: AnalyticsData, filters: FilterContext): void {
    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();
    const margin = 20;
    const contentWidth = pageWidth - (margin * 2);

    let yPos = 20;

    // Header - Title
    doc.setFillColor(33, 150, 243);
    doc.rect(0, 0, pageWidth, 35, 'F');
    
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(22);
    doc.setFont('helvetica', 'bold');
    doc.text('Test Analytics Report', pageWidth / 2, 18, { align: 'center' });
    
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');
    const dateStr = new Date().toLocaleString();
    doc.text(`Generated: ${dateStr}`, pageWidth / 2, 28, { align: 'center' });
    
    yPos = 45;

    // Filters section
    const filterTexts: string[] = [];
    if (filters.user && filters.user !== 'all') {
      filterTexts.push(`User: ${filters.user}`);
    }
    if (filters.project && filters.project !== 'all') {
      filterTexts.push(`Project: ${filters.project}`);
    }
    if (filters.module && filters.module !== 'all') {
      filterTexts.push(`Module: ${filters.module}`);
    }

    if (filterTexts.length > 0) {
      doc.setTextColor(100, 100, 100);
      doc.setFontSize(10);
      doc.setFont('helvetica', 'italic');
      doc.text(`Filters: ${filterTexts.join(' | ')}`, pageWidth / 2, yPos, { align: 'center' });
      yPos += 15;
    }

    // Summary Section with border (matching module breakdown style)
    const summaryStartY = yPos;
    const summaryTitleHeight = 12;
    const summaryRowHeight = 10;
    const summaryBottomPadding = 5;
    
    // Summary data with colors (defined before calculating height)
    const summaryItems = [
      { label: 'Total Test Cases', value: data.totalTestCases, color: [51, 51, 51] },
      { label: 'Executed', value: data.executedCount, percent: this.calculatePercent(data.executedCount, data.totalTestCases), color: [33, 150, 243] },
      { label: 'Passed', value: data.passedCount, percent: data.passRate, color: [76, 175, 80] },
      { label: 'Failed', value: data.failedCount, percent: data.failRate, color: [244, 67, 54] },
      { label: 'Not Executed', value: data.notExecutedCount, percent: this.calculatePercent(data.notExecutedCount, data.totalTestCases), color: [158, 158, 158] }
    ];
    
    const totalSummaryHeight = summaryTitleHeight + (summaryItems.length * summaryRowHeight) + summaryBottomPadding;
    
    // Section title
    doc.setTextColor(33, 150, 243);
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Summary', margin + 5, yPos + 8);
    yPos += summaryTitleHeight;

    summaryItems.forEach((item, index) => {
      const rowY = yPos + (index * summaryRowHeight);
      
      doc.setTextColor(item.color[0], item.color[1], item.color[2]);
      doc.setFontSize(11);
      doc.setFont('helvetica', index === 0 ? 'bold' : 'normal');
      doc.text(item.label, margin + 5, rowY + 7);
      
      const valueText = item.percent !== undefined 
        ? `${item.value} (${item.percent}%)` 
        : item.value.toString();
      doc.text(valueText, margin + 65, rowY + 7);
    });

    // Draw border last (matching module breakdown style)
    yPos = summaryStartY;
    doc.setDrawColor(33, 150, 243);
    doc.setLineWidth(1);
    doc.rect(margin, yPos, contentWidth, totalSummaryHeight);

    yPos = summaryStartY + totalSummaryHeight + 10;

    // Project Header - Always show project context
    const projectLabel = (filters.project && filters.project !== 'all') 
      ? `Project: ${filters.project}` 
      : 'All Projects';
    
    doc.setFillColor(76, 175, 80);
    doc.rect(margin, yPos - 5, contentWidth, 12, 'F');
    
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.text(projectLabel, margin + 5, yPos + 3);
    yPos += 15;

    // Module Breakdown Section
    if (data.byModule && data.byModule.length > 0) {
      const tableStartY = yPos;
      const sectionTitleHeight = 12;
      const headerHeight = 10;
      const rowHeight = 10;
      const bottomPadding = 5;
      const totalTableHeight = sectionTitleHeight + headerHeight + (data.byModule.length * rowHeight) + bottomPadding;

      // Section title
      doc.setTextColor(33, 150, 243);
      doc.setFontSize(14);
      doc.setFont('helvetica', 'bold');
      doc.text('Module Breakdown', margin + 5, yPos + 8);
      yPos += sectionTitleHeight;

      // Table header row background
      doc.setFillColor(33, 150, 243);
      doc.rect(margin, yPos, contentWidth, headerHeight, 'F');
      
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(9);
      doc.setFont('helvetica', 'bold');
      
      const tableHeaders = ['Module', 'Total', 'Passed', 'Failed', 'Not Executed'];
      const tableColWidths = [65, 25, 25, 25, 35];
      let xPos = margin + 3;
      tableHeaders.forEach((header, i) => {
        doc.text(header, xPos, yPos + 7);
        xPos += tableColWidths[i];
      });
      yPos += headerHeight;

      // Table rows
      doc.setFont('helvetica', 'normal');
      data.byModule.forEach((module, index) => {
        const rowY = yPos + (index * rowHeight);
        
        // Alternate row colors
        if (index % 2 === 0) {
          doc.setFillColor(245, 245, 245);
          doc.rect(margin, rowY, contentWidth, rowHeight, 'F');
        }

        const rowData = [
          this.truncateText(module.moduleName, 30),
          module.totalTestCases.toString(),
          module.passedCount.toString(),
          module.failedCount.toString(),
          module.notExecutedCount.toString()
        ];

        const colors = [
          [51, 51, 51],
          [51, 51, 51],
          [76, 175, 80],
          [244, 67, 54],
          [158, 158, 158]
        ];

        xPos = margin + 3;
        rowData.forEach((cell, i) => {
          doc.setTextColor(colors[i][0], colors[i][1], colors[i][2]);
          doc.setFontSize(8);
          doc.text(cell, xPos, rowY + 7);
          xPos += tableColWidths[i];
        });
      });

      // Draw border last (after all content)
      yPos = tableStartY;
      doc.setDrawColor(33, 150, 243);
      doc.setLineWidth(1);
      doc.rect(margin, yPos, contentWidth, totalTableHeight);
    } else {
      doc.setTextColor(158, 158, 158);
      doc.setFontSize(11);
      doc.setFont('helvetica', 'italic');
      doc.text('No module data available.', margin + 5, yPos + 10);
    }

    // Footer
    const pageHeight = doc.internal.pageSize.getHeight();
    doc.setTextColor(150, 150, 150);
    doc.setFontSize(8);
    doc.text('Test Case Management System', pageWidth / 2, pageHeight - 10, { align: 'center' });

    const fileName = `test-analytics-${this.formatDateForFileName(new Date())}.pdf`;
    doc.save(fileName);
  }

  private calculatePercent(value: number, total: number): string {
    if (total === 0) return '0';
    return ((value / total) * 100).toFixed(1);
  }

  private truncateText(text: string, maxLength: number): string {
    if (!text) return '';
    return text.length > maxLength ? text.substring(0, maxLength - 2) + '..' : text;
  }

  private formatDateForFileName(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
