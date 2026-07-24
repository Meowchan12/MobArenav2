package vn.meowchan12.mobarenav2.utils;

public class ArenaMath {

    /**
     * Thuật toán: Tính số lượng quái sẽ sinh ra.
     * Cứ thêm 1 người chơi, lượng quái tăng 50%, nhưng giới hạn tối đa chỉ nhân 3.5 lần (tương đương 6 người chơi).
     * Để tránh việc 10-20 người chơi làm crash server vì quá nhiều Entity.
     */
    public static int calculateSpawnAmount(int baseAmount, int playerCount, double customMultiplier) {
        if (playerCount <= 1) return (int) (baseAmount * customMultiplier);

        // Công thức: Base * Multiplier * (1 + (Players - 1) * 0.5)
        double scalingFactor = 1.0 + ((playerCount - 1) * 0.5);

        // Đặt mức trần tối đa là x3.5 lượng quái gốc
        scalingFactor = Math.min(scalingFactor, 3.5);

        return (int) Math.floor(baseAmount * customMultiplier * scalingFactor);
    }

    /**
     * Thuật toán: Tính máu Boss thông minh.
     * Sử dụng HP gốc từ file bosses.yml, người chơi đầu tiên chịu 100% HP.
     * Từ người chơi thứ 2 trở đi, mỗi người chỉ cộng thêm 30% HP gốc cho Boss.
     * Giúp Boss trâu hơn nhưng không biến thành "bịch bông" tốn thời gian.
     */
    public static double calculateBossHealth(double baseHpFromConfig, double customMultiplier, int playerCount) {
        if (playerCount <= 1) return baseHpFromConfig * customMultiplier;

        // Công thức: BaseHP * Multiplier * (100% + (Players - 1) * 30%)
        double healthScaling = 1.0 + ((playerCount - 1) * 0.30);

        return baseHpFromConfig * customMultiplier * healthScaling;
    }
}