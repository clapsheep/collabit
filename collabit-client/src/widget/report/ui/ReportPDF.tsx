import ReportPDFHeader from "@/entities/report/ui/ReportPDFHeader";
import ScorePdfSection from "@/features/pdf/ui/ScorePdfSection";
import { useImperativeHandle, useRef } from "react";
import useReport from "@/features/report/api/useReport";
import html2pdf from "html2pdf.js";
import { forwardRef } from "react";
import CloudPdfSection from "@/features/pdf/ui/CloudPdfSection";
import AISummaryPdfSection from "@/features/pdf/ui/AISummaryPdfSection";
import { useToast } from "@/shared/hooks/use-toast";

interface ReportPDFProps {
  shareUrl: string;
}

const ReportPDF = forwardRef(({ shareUrl }: ReportPDFProps, ref) => {
  const { hexagon, progress, wordCloud, aiSummary } = useReport();
  const contentRef = useRef<HTMLDivElement>(null);
  const { toast } = useToast();

  useImperativeHandle(ref, () => ({ handleDownloadPDF }));
  const handleDownloadPDF = () => {
    console.log("📄 PDF 다운로드 요청 중...");
    console.log("📄 contentRef.current:", contentRef.current);

    const element = contentRef.current;
    if (!element) {
      toast({
        title: "PDF 다운로드 실패",
        description: "PDF 파일 다운로드에 실패했습니다.",
      });
      return;
    }

    const options = {
      filename: "report.pdf",
      image: { type: "jpeg", quality: 1 },
      html2canvas: {
        scale: 3,
        useCORS: true,
        letterRendering: true,
        backgroundColor: null,
      },
      jsPDF: { unit: "mm", format: "a4", orientation: "portrait" },
    };

    html2pdf()
      .set(options)
      .from(element)
      .save()
      .then(() => {
        toast({
          title: "PDF 다운로드 완료",
          description: "PDF 파일이 다운로드되었습니다.",
        });
      })
      .catch(() =>
        toast({
          title: "PDF 다운로드 실패",
          description: "PDF 파일 다운로드에 실패했습니다.",
        }),
      );
  };
  return (
    <div className="absolute left-[-9999px]">
      <div
        ref={contentRef}
        className="mx-auto h-[287mm] w-[200mm] bg-white p-[5mm]"
      >
        <ReportPDFHeader shareUrl={shareUrl} />
        {hexagon && progress && (
          <ScorePdfSection hexagon={hexagon} progress={progress} />
        )}
        {wordCloud && (
          <CloudPdfSection
            strength={wordCloud.strength}
            weakness={wordCloud.weakness}
          />
        )}
        {aiSummary && (
          <AISummaryPdfSection
            strength={aiSummary.strength}
            weakness={aiSummary.weakness}
          />
        )}
      </div>
    </div>
  );
});

ReportPDF.displayName = "ReportPDF";

export default ReportPDF;
