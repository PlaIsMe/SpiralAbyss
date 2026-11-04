package com.pla.spiral_abyss.client.renderer;

/** Original, palette-quantized pixel animation. No vanilla portal artwork or smooth geometry. */
final class PortalPixels {
    static final int WIDTH = 64, HEIGHT = 72, FRAMES = 64;
    private static final double TAU = Math.PI * 2;
    private static final int[] PALETTE = {
            0x030B22, 0x071A3D, 0x0C2E63, 0x124C8D, 0x176DB5,
            0x258FD3, 0x39B9EC, 0x69E3FF, 0xA6F3FF
    };
    // Actual stepped opening in texture pixels: shoulder, straight posts, lower steps and center sill.
    private static final int[] OUTLINE_X = {8, 56, 56, 64, 64, 56, 56, 40, 40, 24, 24, 8, 8, 0, 0, 8};
    private static final int[] OUTLINE_Y = {0, 0, 8, 8, 56, 56, 64, 64, 72, 72, 64, 64, 56, 56, 8, 8};
    private static final double[] BORDER_DISTANCE = new double[WIDTH * HEIGHT];
    private static final double[] BORDER_PHASE = new double[WIDTH * HEIGHT];
    static {
        double perimeter = 0;
        for (int i = 0; i < OUTLINE_X.length; i++) {
            int next = (i + 1) % OUTLINE_X.length;
            perimeter += Math.hypot(OUTLINE_X[next] - OUTLINE_X[i], OUTLINE_Y[next] - OUTLINE_Y[i]);
        }
        for (int y = 0; y < HEIGHT; y++) for (int x = 0; x < WIDTH; x++) {
            double best = Double.POSITIVE_INFINITY, along = 0, selected = 0;
            for (int i = 0; i < OUTLINE_X.length; i++) {
                int next = (i + 1) % OUTLINE_X.length;
                double dx = OUTLINE_X[next] - OUTLINE_X[i], dy = OUTLINE_Y[next] - OUTLINE_Y[i];
                double length = Math.hypot(dx, dy);
                double t = Math.max(0, Math.min(1,
                        ((x + .5 - OUTLINE_X[i]) * dx + (y + .5 - OUTLINE_Y[i]) * dy) / (length * length)));
                double distance = Math.hypot(x + .5 - OUTLINE_X[i] - t * dx, y + .5 - OUTLINE_Y[i] - t * dy);
                if (distance < best) { best = distance; selected = along + t * length; }
                along += length;
            }
            BORDER_DISTANCE[y * WIDTH + x] = best;
            BORDER_PHASE[y * WIDTH + x] = selected / perimeter * TAU;
        }
    }
    private PortalPixels() {}

    /** NativeImage uses ABGR. States: transparent idle, occupied vortex, entry surge. */
    static int pixel(int x, int y, int state, int frame) {
        if (!inside(x, y)) return 0;
        double px = (x + .5 - 32) / 32;
        double py = (35.2 - y - .5) / 32;
        double radius = Math.sqrt(px * px + py * py);
        double angle = Math.atan2(py, px);
        double time = Math.floorMod(frame, FRAMES) * TAU / FRAMES;
        double distance = BORDER_DISTANCE[y * WIDTH + x];
        double flow = BORDER_PHASE[y * WIDTH + x] - time;
        if (state == 0) return idle(x, y, distance, flow, time);
        double rolling = .5 + .5 * Math.sin(flow * 4 + Math.sin(flow * 3) * .7);
        double ripple = Math.sin(flow * 7 + time * 2) * .7;
        boolean active = state > 0;
        int shade = 0;

        if (active) {
            // A few broad hooked currents pull into a dark center; integer palette steps keep pixel edges.
            double spiral = .5 + .5 * Math.sin(angle * 4 + radius * 8 - time * (state == 2 ? 8 : 4)
                    + Math.sin(angle * 3 - time * 3) * .55);
            double cloud = .5 + .5 * Math.sin(angle * 9 + radius * 15 + time * 2);
            double well = Math.max(0, Math.min(1, (radius - .12) / .75));
            shade = (int) (well * (1.1 + Math.pow(spiral, 3) * (state == 2 ? 4.5 : 3.5) + cloud * .65));
        }

        // Follow every straight edge and step. Travelling broad wave heads curl inward from this contour.
        double width = (active ? 3.2 : 1.4) + rolling * (active ? 6.2 : 3.8) + ripple;
        if (distance < width) {
            double band = distance / width;
            int rim = band < .18 ? 4 : band < .52 ? 7 : band < .78 ? 5 : 3;
            rim += rolling > .68 ? 1 : rolling < .2 ? -2 : 0;
            shade = Math.max(shade, rim);
        } else if (!active) {
            // Sparse single-pixel blue flecks follow the frame, never a star-filled interior.
            int grain = Math.floorMod(x * 73 + y * 151, 43);
            if (distance < width + 1.5 && grain == 0 && rolling > .55) shade = 5;
            else return 0;
        }

        // Small deterministic neighboring steps break up broad flat bands without temporal flicker.
        if (shade >= 3 && shade <= 6 && Math.floorMod(x * 13 + y * 7, 11) == 0) shade--;
        int rgb = PALETTE[Math.max(0, Math.min(PALETTE.length - 1, shade))];
        return 0xFF000000 | ((rgb & 255) << 16) | (rgb & 0xFF00) | (rgb >> 16);
    }

    private static int idle(int x, int y, double distance, double flow, double time) {
        // Broad translucent sheets roll along the frame. Their hooked inner edges leave the view open.
        double head = .5 + .5 * Math.sin(flow * 3 + Math.sin(flow * 2) * .8);
        double eddy = .5 + .5 * Math.sin(flow * 7 - distance * .48 + Math.sin(flow * 3 + time * 2));
        double width = 3.8 + head * 7.8 + Math.sin(flow * 9 + time * 2) * .85;
        if (distance >= width || distance > 12) return 0;
        double band = distance / width;
        double curl = .5 + .5 * Math.sin(flow * 5 - distance * .72 + time * 2);
        int alpha;
        int shade;
        if (band < .16) { alpha = 150; shade = 5; }
        else if (band < .40) { alpha = head > .58 ? 235 : 185; shade = head > .55 ? 8 : 6; }
        else if (band < .70) { alpha = 140; shade = eddy > .45 ? 6 : 4; }
        else { alpha = curl > .55 ? 105 : 55; shade = eddy > .62 ? 5 : 3; }
        if (head < .22 && band > .55 && eddy < .38) return 0;
        if (Math.floorMod(x * 13 + y * 7, 11) == 0 && shade > 3) shade--;
        int rgb = PALETTE[shade];
        return (alpha << 24) | ((rgb & 255) << 16) | (rgb & 0xFF00) | (rgb >> 16);
    }

    private static boolean inside(int x, int y) {
        if (y < 8) return x >= 8 && x < 56;
        if (y < 56) return true;
        if (y < 64) return x >= 8 && x < 56;
        return x >= 24 && x < 40;
    }
}
