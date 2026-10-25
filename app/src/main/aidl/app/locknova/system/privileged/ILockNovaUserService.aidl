package app.locknova.system.privileged;

interface ILockNovaUserService {
    String readAnimationScale(String key);
    void writeAnimationScale(String key, String value);
}
