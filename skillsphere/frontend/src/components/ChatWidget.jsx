import ChatWorkspace from "./ChatWorkspace.jsx";

export default function ChatWidget({ currentUserId, defaultTargetUserId = null, isManager = false }) {
  return (
    <ChatWorkspace
      currentUserId={currentUserId}
      defaultTargetUserId={defaultTargetUserId}
      isManager={isManager}
    />
  );
}
