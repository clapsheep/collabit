import CommunityDetail from "@/features/community/ui/CommunityDetail";
import { getPostAPI } from "@/shared/api/community";

const CommunityDetailPage = async ({
  params,
}: {
  params: Promise<{ postId: string }>;
}) => {
  const { postId } = await params;
  const post = await getPostAPI(Number(postId));
  console.log(post);

  return (
    <div className="mx-auto max-w-5xl overflow-y-auto">
      <CommunityDetail post={post} />
    </div>
  );
};

export default CommunityDetailPage;
