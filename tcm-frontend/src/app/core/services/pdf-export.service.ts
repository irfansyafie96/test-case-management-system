import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

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

  private static readonly COLORS = {
    passed: '#4CAF50',
    failed: '#F44336',
    notExecuted: '#9E9E9E',
    primary: '#2196F3',
    headerBg: [33, 150, 243]
  } as const;

  private static readonly CHART_CONFIG = {
    pie: {
      width: 400,
      height: 250
    }
  } as const;

  private static readonly STYLES = {
    primary: '#1a1a1a',
    secondary: '#666666',
    lightGray: '#f5f5f5',
    mediumGray: '#e0e0e0',
    border: '#dddddd',
    passed: '#22c55e',
    failed: '#ef4444',
    notExecuted: '#9ca3af'
  } as const;

  private static readonly LAYOUT = {
    margin: 20,
    headerHeight: 25,
    sectionGap: 15,
    rowHeight: 9,
    headerRowHeight: 10
  } as const;

  constructor() { }

  private createPieChartImage(data: AnalyticsData): string {
    const total = data.totalTestCases || 1;
    const passedPercent = ((data.passedCount / total) * 100).toFixed(1);
    const failedPercent = ((data.failedCount / total) * 100).toFixed(1);
    const notExecutedPercent = ((data.notExecutedCount / total) * 100).toFixed(1);

    const canvas = document.createElement('canvas');
    canvas.width = PdfExportService.CHART_CONFIG.pie.width;
    canvas.height = PdfExportService.CHART_CONFIG.pie.height;
    
    const chart = new Chart(canvas, {
      type: 'pie',
      data: {
        labels: [`Passed (${passedPercent}%)`, `Failed (${failedPercent}%)`, `Not Executed (${notExecutedPercent}%)`],
        datasets: [{
          data: [data.passedCount, data.failedCount, data.notExecutedCount],
          backgroundColor: [
            PdfExportService.COLORS.passed,
            PdfExportService.COLORS.failed,
            PdfExportService.COLORS.notExecuted
          ],
          borderWidth: 0
        }]
      },
      options: {
        responsive: false,
        animation: false,
        plugins: {
          legend: {
            position: 'right',
            labels: {
              font: { size: 12, family: 'Helvetica' },
              padding: 15,
              usePointStyle: true,
              pointStyle: 'circle'
            }
          }
        }
      }
    });

    const image = chart.toBase64Image();
    chart.destroy();
    return image;
  }

  exportAnalyticsReport(data: AnalyticsData, filters: FilterContext): void {
    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();
    const margin = PdfExportService.LAYOUT.margin;
    const contentWidth = pageWidth - (margin * 2);

    let yPos = margin;

    // Header - Clean minimal design
    doc.setDrawColor(PdfExportService.STYLES.mediumGray);
    doc.setLineWidth(0.5);
    doc.line(margin, yPos + PdfExportService.LAYOUT.headerHeight, pageWidth - margin, yPos + PdfExportService.LAYOUT.headerHeight);
    
    doc.setTextColor(PdfExportService.STYLES.primary);
    doc.setFontSize(18);
    doc.setFont('helvetica', 'bold');
    doc.text('Test Analytics Report', margin, yPos + 10);
    
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(PdfExportService.STYLES.secondary);
    const dateStr = new Date().toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
    doc.text(dateStr, pageWidth - margin, yPos + 10, { align: 'right' });
    
    yPos += PdfExportService.LAYOUT.headerHeight + 5;

    // Filters section - Inline and subtle
    const filterParts: string[] = [];
    if (filters.project && filters.project !== 'all') {
      filterParts.push(`Project: ${filters.project}`);
    }
    if (filters.module && filters.module !== 'all') {
      filterParts.push(`Module: ${filters.module}`);
    }
    if (filters.user && filters.user !== 'all') {
      filterParts.push(`User: ${filters.user}`);
    }

    if (filterParts.length > 0) {
      doc.setFontSize(9);
      doc.setFont('helvetica', 'italic');
      doc.setTextColor(PdfExportService.STYLES.secondary);
      doc.text(filterParts.join('  •  '), margin, yPos + 4);
      yPos += 12;
    }

    // Summary Stats - Clean horizontal layout
    const stats = [
      { label: 'Total', value: data.totalTestCases.toString() },
      { label: 'Executed', value: `${data.executedCount} (${this.calculatePercent(data.executedCount, data.totalTestCases)}%)` },
      { label: 'Passed', value: `${data.passedCount} (${data.passRate.toFixed(1)}%)`, color: PdfExportService.STYLES.passed },
      { label: 'Failed', value: `${data.failedCount} (${data.failRate.toFixed(1)}%)`, color: PdfExportService.STYLES.failed },
      { label: 'Not Executed', value: `${data.notExecutedCount} (${this.calculatePercent(data.notExecutedCount, data.totalTestCases)}%)`, color: PdfExportService.STYLES.notExecuted }
    ];

    const colWidth = contentWidth / stats.length;
    stats.forEach((stat, i) => {
      const x = margin + (i * colWidth);
      
      doc.setFontSize(8);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(PdfExportService.STYLES.secondary);
      doc.text(stat.label.toUpperCase(), x + colWidth/2, yPos + 4, { align: 'center' });
      
      doc.setFontSize(11);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(stat.color || PdfExportService.STYLES.primary);
      doc.text(stat.value, x + colWidth/2, yPos + 11, { align: 'center' });
    });

    yPos += 22;

    // Pie Chart Section
    const chartImage = this.createPieChartImage(data);
    const chartWidth = 100;
    const chartHeight = 65;
    const chartX = margin;
    
    doc.addImage(chartImage, 'PNG', chartX, yPos, chartWidth, chartHeight);
    
    // Module Breakdown - Next to chart
    const tableX = margin + chartWidth + 15;
    const tableWidth = pageWidth - tableX - margin;
    
    if (data.byModule && data.byModule.length > 0) {
      doc.setFontSize(10);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(PdfExportService.STYLES.primary);
      doc.text('Module Breakdown', tableX, yPos + 5);
      yPos += 10;

      const headerHeight = PdfExportService.LAYOUT.headerRowHeight;
      const rowHeight = PdfExportService.LAYOUT.rowHeight;
      const tableHeaderY = yPos;
      
      // Table header - light gray background
      doc.setFillColor(PdfExportService.STYLES.lightGray);
      doc.rect(tableX, tableHeaderY, tableWidth, headerHeight, 'F');
      
      doc.setFontSize(8);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(PdfExportService.STYLES.secondary);
      
      const tableHeaders = ['Module', 'Total', 'Pass', 'Fail', 'N/A'];
      const colWidths = [0.40, 0.15, 0.15, 0.15, 0.15];
      let xOffset = tableX + 3;
      tableHeaders.forEach((header, i) => {
        doc.text(header, xOffset, tableHeaderY + 6.5);
        xOffset += tableWidth * colWidths[i];
      });
      
      yPos = tableHeaderY + headerHeight;

      // Table rows
      doc.setFont('helvetica', 'normal');
      data.byModule.forEach((module, idx) => {
        const rowY = yPos + (idx * rowHeight);
        
        // Alternate row background
        if (idx % 2 === 0) {
          doc.setFillColor(250, 250, 250);
          doc.rect(tableX, rowY, tableWidth, rowHeight, 'F');
        }
        
        const rowData = [
          this.truncateText(module.moduleName, 20),
          module.totalTestCases.toString(),
          module.passedCount.toString(),
          module.failedCount.toString(),
          module.notExecutedCount.toString()
        ];
        
        xOffset = tableX + 3;
        rowData.forEach((cell, i) => {
          doc.setFontSize(8);
          if (i === 2) doc.setTextColor(PdfExportService.STYLES.passed);
          else if (i === 3) doc.setTextColor(PdfExportService.STYLES.failed);
          else doc.setTextColor(PdfExportService.STYLES.primary);
          
          doc.text(cell, xOffset, rowY + 6);
          xOffset += tableWidth * colWidths[i];
        });
      });

      // Table border
      const tableHeight = headerHeight + (data.byModule.length * rowHeight);
      doc.setDrawColor(PdfExportService.STYLES.mediumGray);
      doc.setLineWidth(0.3);
      doc.rect(tableX, tableHeaderY, tableWidth, tableHeight);
    }

    yPos += Math.max(chartHeight, (data.byModule?.length || 0) * PdfExportService.LAYOUT.rowHeight + 25) + 10;

    // Footer
    const pageHeight = doc.internal.pageSize.getHeight();
    doc.setDrawColor(PdfExportService.STYLES.mediumGray);
    doc.setLineWidth(0.3);
    doc.line(margin, pageHeight - 12, pageWidth - margin, pageHeight - 12);
    
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(PdfExportService.STYLES.secondary);
    doc.text('Test Case Management System', margin, pageHeight - 7);
    doc.text('Page 1', pageWidth - margin, pageHeight - 7, { align: 'right' });

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
