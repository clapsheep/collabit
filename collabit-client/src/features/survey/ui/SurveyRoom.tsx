"use client";
import ChatHeader from "@/entities/chat/ui/ChatHeader";
import ChatInput from "@/entities/chat/ui/ChatInput";
import SurveyBubble from "@/entities/survey/ui/SurveyBubble";
import SurveyMultipleSelectButton from "@/entities/survey/ui/SurveyMultipleSelectButton";
import { useAuth } from "@/features/auth/api/useAuth";
import {
  essaySurveyProgressAPI,
  getSurveyMultipleQueryAPI,
  startEssaySurveyAPI,
} from "@/shared/api/survey";
import { useSurveyStore } from "@/shared/lib/stores/surveyStore";
import { Button } from "@/shared/ui/button";
import generateGreetingMessage from "@/shared/utils/generateGreetingMessage";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import useSendMultipleAnswer from "../api/useSendMultipleAnswer";
import essaySurveyAPI from "@/entities/survey/api/essaySurvey";

const SurveyRoom = ({ id }: { id: number }) => {
  const { userInfo } = useAuth();

  /* 초기 디테일 데이터 불러오기 */
  const setId = useSurveyStore((state) => state.setId);
  const surveyDetail = useSurveyStore((state) => state.surveyDetail);
  const surveyEssayResponse = useSurveyStore(
    (state) => state.surveyEssayResponse,
  );
  const surveyMultipleResponse = useSurveyStore(
    (state) => state.surveyMultipleResponse,
  );
  const multipleAnswers = useSurveyStore((state) => state.multipleAnswers);
  console.log(surveyMultipleResponse);
  console.log(surveyEssayResponse);

  // 디테일 스토어 업데이트
  useEffect(() => {
    setId(id);
  }, [id, setId]);

  /* 설문 스텝 관리 */
  const [currentStep, setCurrentStep] = useState(-1);
  const [mutipleStep, setMutipleStep] = useState(0);

  // 객관식 설문 리스트
  const { data: surveyMultipleQuery } = useQuery({
    queryKey: ["surveyMultiple"],
    queryFn: () => getSurveyMultipleQueryAPI(),
    enabled: !!userInfo && !!id,
    staleTime: 1000 * 60,
  });
  const { sendMultipleAnswer } = useSendMultipleAnswer();

  const handleStartMultiple = () => {
    setCurrentStep(0);
  };

  const handleSelectAnswer = (currentIndex: number) => {
    console.log(currentIndex, currentStep);
    if (currentIndex === currentStep) {
      setCurrentStep(currentIndex + 1);
      if (mutipleStep < 23) {
        setMutipleStep(mutipleStep + 1);
      }
    }
  };

  type EssayMessage = {
    role: "assistant" | "user";
    content: string;
    timestamp?: string;
  };
  // 주관식 설문 시작
  const [inputMessage, setInputMessage] = useState("");
  const [essayMessageList, setEssayMessageList] = useState<EssayMessage[]>([]);

  const [currentMessage, setCurrentMessage] = useState("");
  const [essayStatus, setEssayStatus] = useState<
    "READY" | "PENDING" | "STREAMING" | "COMPLETED" | "ERROR"
  >("READY");
  const handleSendButton = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (essayStatus === "STREAMING" || essayStatus === "PENDING") {
      return;
    }

    setEssayMessageList((prevList) => [
      ...prevList,
      { role: "assistant", content: currentMessage },
      { role: "user", content: inputMessage },
    ]);

    // 메시지 초기화는 한번에 처리
    setCurrentMessage("");
    setInputMessage("");

    essaySurveyAPI({
      surveyCode: id,
      body: inputMessage,
      api: essaySurveyProgressAPI,
      setState: setCurrentMessage,
      setStatus: setEssayStatus,
    });
  };

  const handleEndMultiple = async () => {
    setCurrentStep((prev) => prev + 1);
    sendMultipleAnswer({
      surveyCode: id,
      answer: multipleAnswers,
    });

    await essaySurveyAPI({
      surveyCode: id,
      body: surveyDetail?.nickname as string,
      api: startEssaySurveyAPI,
      setState: setCurrentMessage,
      setStatus: setEssayStatus,
    });
  };

  // 유저 정보가 없거나 디테일이 없으면 렌더링 안함
  if (!userInfo || !surveyDetail) {
    return null;
  }
  return (
    <div className="flex h-screen w-full flex-col gap-3 py-4 md:h-[calc(100vh-108px)] md:px-2">
      <ChatHeader
        nickname={surveyDetail?.nickname}
        projectName={surveyDetail?.title}
        profileImage={surveyDetail?.profileImage}
      />
      <div className="flex h-full w-full flex-col-reverse overflow-y-auto bg-white px-2 md:h-[calc(100vh-256px)] md:px-4 md:py-3">
        <div className="flex flex-col-reverse gap-6">
          {/* 주관식 설문 실시간 메시지 */}
          {(essayStatus === "STREAMING" ||
            essayStatus === "COMPLETED" ||
            essayStatus === "PENDING") && (
            <SurveyBubble
              isMe={false}
              message={currentMessage}
              isLoading={essayStatus === "PENDING"}
              animation={false}
            />
          )}

          {/* 주관식 설문 메시지 */}
          <div className="flex flex-col gap-6">
            {essayMessageList.map((item, index) => {
              return (
                <SurveyBubble
                  key={index}
                  isMe={item.role === "user"}
                  message={item.content}
                  isLoading={false}
                  animation={false}
                />
              );
            })}
          </div>
          {/*객관식 마지막 메시지 */}

          {currentStep >= 24 ||
            (surveyMultipleResponse && (
              <SurveyBubble
                isMe={false}
                message={`감사합니다! ${userInfo.nickname}님의 이야기를 들으니까 ${surveyDetail?.nickname}님이 어떤 사람인지는 조금 알 것 같아요. \n\n 이제 더 구체적으로 알고 싶은데, 대화로 알려주시겠어요? (지금 나가시면 피드백이 완료되지 않아요!)`}
                animation={currentStep === 24}
                isLoading={false}
                component={
                  <Button
                    disabled={currentStep > 24 || !!surveyEssayResponse}
                    className="duration-900 animate-in fade-in-0 slide-in-from-bottom-4"
                    onClick={handleEndMultiple}
                  >
                    대화 시작하기
                  </Button>
                }
              />
            ))}
          {/* 우선 스텝에 따른 챗봇의 메세지 렌더링 */}

          {!surveyMultipleResponse ? ( // 객관식 설문한 적이 없으면면
            <>
              {surveyMultipleQuery
                ?.slice(0, currentStep + 1)
                .reverse()
                .map((item, index) => {
                  const reversedIndex = currentStep - index;
                  return (
                    <SurveyBubble
                      key={item.questionNumber}
                      isMe={false}
                      step={mutipleStep - index + 1}
                      message={
                        reversedIndex === 0
                          ? `감사합니다, 그러면 몇가지 질문을 드릴게요! 5가지 이모티콘 중 제가 드리는 질문에 가장 적합한 이모티콘을 눌러주세요! 첫번째 질문을 시작하겠습니다. ${surveyDetail?.nickname}${item.questionText}`
                          : `${surveyDetail?.nickname}${item.questionText}`
                      }
                      isLoading={false}
                      component={
                        <SurveyMultipleSelectButton
                          index={reversedIndex}
                          onClick={() => handleSelectAnswer(reversedIndex)}
                        />
                      }
                      animation={reversedIndex === currentStep}
                    />
                  );
                })}
              {/* 시작 메시지 */}
              <SurveyBubble
                isMe={false}
                message={generateGreetingMessage(
                  userInfo.nickname,
                  surveyDetail?.nickname ?? "",
                  surveyDetail?.title ?? "",
                )}
                animation={currentStep === -1}
                isLoading={false}
                component={
                  <Button
                    disabled={currentStep >= 0}
                    className="fade-in-duration-700 duration-700 animate-in fade-in-0 slide-in-from-bottom-4"
                    onClick={handleStartMultiple}
                  >
                    시작하기
                  </Button>
                }
              />
            </>
          ) : (
            // 객관식 설문한 적이 있으면
            <>
              {surveyMultipleQuery
                ?.slice()
                .reverse()
                .map((item, index) => {
                  const originalIndex = surveyMultipleQuery.length - 1 - index;
                  return (
                    <SurveyBubble
                      key={item.questionNumber}
                      isMe={false}
                      step={originalIndex + 1}
                      message={`${item.questionText}`}
                      isLoading={false}
                      component={
                        <SurveyMultipleSelectButton
                          index={originalIndex}
                          selectedScore={surveyMultipleResponse[originalIndex]}
                          readOnly
                          onClick={() => {}}
                        />
                      }
                      animation={false}
                    />
                  );
                })}
              {/* 완료된 설문의 시작 메시지 */}
              <SurveyBubble
                isMe={false}
                message={generateGreetingMessage(
                  userInfo.nickname,
                  surveyDetail?.nickname ?? "",
                  surveyDetail?.title ?? "",
                )}
                animation={false}
                isLoading={false}
                component={
                  <Button
                    disabled={
                      currentStep >= 0 || surveyMultipleResponse.length > 0
                    }
                    className="fade-in-duration-700 duration-700 animate-in fade-in-0 slide-in-from-bottom-4"
                    onClick={handleStartMultiple}
                  >
                    시작하기
                  </Button>
                }
              />
            </>
          )}
        </div>
      </div>
      <ChatInput
        disabled={currentStep < 25}
        message={inputMessage}
        setInputMessage={setInputMessage}
        handleSendMessage={handleSendButton}
      />
    </div>
  );
};

export default SurveyRoom;
