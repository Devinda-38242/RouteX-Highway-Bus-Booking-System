package com.routex.bot;

import com.routex.entity.BusRoute;
import com.routex.repository.BusRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode")
@Service
@RequiredArgsConstructor
public class ChatBotService {

    private final BusRouteRepository routeRepository;

    // City alias map: alias → canonical DB city name
    private static final Map<String, String> CITY_ALIASES = new LinkedHashMap<>();
    static {
        CITY_ALIASES.put("pettah",             "Colombo (Pettah)");
        CITY_ALIASES.put("colombo pettah",     "Colombo (Pettah)");
        CITY_ALIASES.put("makumbura",          "Makumbura (Colombo)");
        CITY_ALIASES.put("makubura",           "Makumbura (Colombo)");
        CITY_ALIASES.put("kataragama",         "Katharagama");
        CITY_ALIASES.put("katharagama",        "Katharagama");
        CITY_ALIASES.put("kandy",              "Kandy");
        CITY_ALIASES.put("galle",              "Galle");
        CITY_ALIASES.put("jaffna",             "Jaffna");
        CITY_ALIASES.put("matara",             "Matara");
        CITY_ALIASES.put("anuradhapura",       "Anuradhapura");
        CITY_ALIASES.put("trincomalee",        "Trincomalee");
        CITY_ALIASES.put("badulla",            "Badulla");
        CITY_ALIASES.put("batticaloa",         "Batticaloa");
        // "colombo" alone defaults to Pettah (most common departure)
        CITY_ALIASES.put("colombo",            "Colombo (Pettah)");
    }

    public String processMessage(String message) {
        String msg = message.toLowerCase().trim();

        // Greeting
        if (matches(msg, "hello|hi|hey|ayubowan|good morning|good evening")) {
            return "👋 Hello! I'm **RouteBot**, your RouteX assistant!\n\nI can help you with:\n" +
                    "• 🔍 Finding bus routes\n• 🕐 Checking departure times\n• 💰 Ticket prices\n" +
                    "• 📞 Operator contact info\n• ❓ General booking help\n\nWhat would you like to know?";
        }

        // "from X to Y" pattern — highest priority
        String[] fromTo = extractFromTo(msg);
        if (fromTo != null) {
            return showRoute(fromTo[0], fromTo[1]);
        }

        // All routes list
        if (matches(msg, "all routes|list routes|available routes|show routes|routes")) {
            return getAllRoutesInfo();
        }

        // Single city mention — show routes to/from it
        String singleCity = resolveSingleCity(msg);
        if (singleCity != null) {
            return showRoutesForCity(singleCity, msg);
        }

        // Price query
        if (matches(msg, "price|cost|fare|how much|ticket price")) {
            return "💰 **Ticket Prices on RouteX**\n\n" +
                    "Prices vary by route and bus type:\n" +
                    "• Express: Rs. 800 – 1,200\n• Luxury: Rs. 1,200 – 1,800\n" +
                    "• Super Luxury: Rs. 1,800 – 2,500\n\n" +
                    "Search your specific route on our homepage to see the exact fare.";
        }

        // Seat availability
        if (matches(msg, "seat|available|availability|how many seats")) {
            return "💺 **Seat Availability**\n\n" +
                    "You can check real-time seat availability by:\n" +
                    "1. Searching your route from the Home page\n" +
                    "2. Clicking **Book Now** on your preferred bus\n" +
                    "3. The **seat map** shows available (green) and booked (red) seats live!\n\n" +
                    "Seats are updated in real-time as bookings happen.";
        }

        // Booking help
        if (matches(msg, "how to book|booking process|how do i book|steps to book")) {
            return "📋 **How to Book on RouteX**\n\n" +
                    "1️⃣ **Register** – Create your account & verify via OTP\n" +
                    "2️⃣ **Search** – Enter From, To, and Date on the Home page\n" +
                    "3️⃣ **Select Bus** – Browse results and click Book Now\n" +
                    "4️⃣ **Choose Seats** – Pick seats from the interactive map\n" +
                    "5️⃣ **Enter Details** – Fill in passenger info & verify OTP\n" +
                    "6️⃣ **Pay** – Complete payment (Card/PayHere)\n" +
                    "7️⃣ **Done!** – Get your QR ticket via email 🎉";
        }

        // Cancellation / refund
        if (matches(msg, "cancel|refund|cancellation")) {
            return "❌ **Cancellation & Refund Policy**\n\n" +
                    "• You can cancel your booking from **My Bookings** in your profile.\n" +
                    "• Refunds are processed within **3-5 business days**.\n" +
                    "• Cancellations made **less than 2 hours** before departure may not be eligible.\n\n" +
                    "Need help? Call us: **075 322 4532 / 011 2034477**";
        }

        // Contact / support
        if (matches(msg, "contact|phone|support|call")) {
            return "📞 **RouteX Support**\n\n" +
                    "**Long Distance Service Inquiries:**\n" +
                    "• 075 322 4532\n• 011 2034477\n\n" +
                    "**Hours:** 24/7 for online queries\n" +
                    "**WhatsApp:** Available 6AM – 10PM\n\n" +
                    "Or email us at: support@routex.lk";
        }

        // QR code
        if (matches(msg, "qr|qr code|scan")) {
            return "📱 **QR Code Tickets on RouteX**\n\n" +
                    "After booking, you'll receive a **QR code ticket** via email.\n\n" +
                    "At the bus bay:\n" +
                    "• Show your QR code to the conductor/driver\n" +
                    "• They'll scan it to verify your booking instantly\n" +
                    "• No paper ticket needed!";
        }

        // Thanks / bye
        if (matches(msg, "thank|thanks|thank you|thx")) {
            return "😊 You're welcome! Have a safe journey with RouteX! 🚌✨";
        }
        if (matches(msg, "bye|goodbye|see you")) {
            return "👋 Goodbye! Safe travels! 🚌";
        }

        // Default
        return "🤔 I'm not sure about that. Try:\n\n" +
                "• **'routes'** – see all available routes\n" +
                "• **'from Colombo to Jaffna'** – find a specific route\n" +
                "• **'price'** – fare information\n" +
                "• **'how to book'** – booking steps\n" +
                "• **'contact'** – support details\n\n" +
                "Or call us at **075 322 4532** 📞";
    }

    /**
     * Extract "from X to Y" or "X to Y" pattern and resolve canonical city names.
     */
    private String[] extractFromTo(String msg) {
        // Pattern 1: "from X to Y" — explicit from/to
        Pattern p1 = Pattern.compile("from\\s+([a-z ()]+?)\\s+to\\s+([a-z ()]+?)(?:\\s*$|\\s+\\d|\\s+on|\\s+at)");
        Matcher m1 = p1.matcher(msg);
        while (m1.find()) {
            String rawFrom = m1.group(1).trim();
            String rawTo   = m1.group(2).trim();
            String from = resolveCity(rawFrom);
            String to   = resolveCity(rawTo);
            if (from != null && to != null && !from.equals(to)) {
                return new String[]{from, to};
            }
        }

        // Pattern 2: "X to Y" without "from" keyword
        Pattern p2 = Pattern.compile("^([a-z ()]+?)\\s+to\\s+([a-z ()]+?)(?:\\s*$|\\s+\\d|\\s+on|\\s+at)");
        Matcher m2 = p2.matcher(msg);
        while (m2.find()) {
            String rawFrom = m2.group(1).trim();
            String rawTo   = m2.group(2).trim();
            String from = resolveCity(rawFrom);
            String to   = resolveCity(rawTo);
            if (from != null && to != null && !from.equals(to)) {
                return new String[]{from, to};
            }
        }

        // Pattern 3: fallback — resolve any two cities in order from the message
        List<String> foundCities = new ArrayList<>();
        // scan message word by word using aliases
        String remaining = msg;
        Map<Integer, String> positions = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : CITY_ALIASES.entrySet()) {
            int idx = remaining.indexOf(e.getKey());
            if (idx >= 0) {
                positions.put(idx, e.getValue());
            }
        }
        List<Map.Entry<Integer, String>> sorted = new ArrayList<>(positions.entrySet());
        sorted.sort(Map.Entry.comparingByKey());
        for (Map.Entry<Integer, String> entry : sorted) {
            String city = entry.getValue();
            if (!foundCities.contains(city)) foundCities.add(city);
            if (foundCities.size() == 2) break;
        }
        if (foundCities.size() == 2) {
            return new String[]{foundCities.get(0), foundCities.get(1)};
        }

        return null;
    }

    /** Resolve a raw text segment to a canonical city name. */
    private String resolveCity(String raw) {
        // Longest-match alias wins
        String best = null;
        int bestLen = 0;
        for (Map.Entry<String, String> e : CITY_ALIASES.entrySet()) {
            if (raw.contains(e.getKey()) && e.getKey().length() > bestLen) {
                best = e.getValue();
                bestLen = e.getKey().length();
            }
        }
        return best;
    }

    /** Resolve first city alias found in the whole message. */
    private String resolveSingleCity(String msg) {
        String best = null;
        int bestLen = 0;
        for (Map.Entry<String, String> e : CITY_ALIASES.entrySet()) {
            if (msg.contains(e.getKey()) && e.getKey().length() > bestLen) {
                best = e.getValue();
                bestLen = e.getKey().length();
            }
        }
        return best;
    }

    private String showRoute(String origin, String destination) {
        List<BusRoute> routes = routeRepository.findByActiveTrue().stream()
                .filter(r -> r.getOrigin().equalsIgnoreCase(origin)
                        && r.getDestination().equalsIgnoreCase(destination))
                .collect(Collectors.toList());

        if (routes.isEmpty()) {
            return "😔 No direct buses found for **" + origin + " → " + destination + "**.\n\n" +
                    "Try searching on our homepage, or ask about a different route!\n" +
                    "Type **'routes'** to see all available routes.";
        }
        return formatRouteList(routes, origin + " → " + destination);
    }

    private String showRoutesForCity(String city, String msg) {
        List<BusRoute> all = routeRepository.findByActiveTrue();

        // Check if context suggests origin vs destination
        boolean hasFrom = msg.contains("from") || msg.contains("departing");
        boolean hasTo   = msg.contains("to") || msg.contains("arriving") || msg.contains("going");

        List<BusRoute> routes;
        String title;

        if (hasFrom && !hasTo) {
            routes = all.stream().filter(r -> r.getOrigin().equalsIgnoreCase(city)).collect(Collectors.toList());
            title = "Routes from " + city;
        } else if (hasTo && !hasFrom) {
            routes = all.stream().filter(r -> r.getDestination().equalsIgnoreCase(city)).collect(Collectors.toList());
            title = "Routes to " + city;
        } else {
            // Show both directions
            routes = all.stream().filter(r -> r.getOrigin().equalsIgnoreCase(city)
                            || r.getDestination().equalsIgnoreCase(city))
                    .collect(Collectors.toList());
            title = "Routes involving " + city;
        }

        if (routes.isEmpty()) {
            return "😔 No routes found for **" + city + "**.\nType **'routes'** to see all available routes.";
        }
        return formatRouteList(routes, title);
    }

    private String formatRouteList(List<BusRoute> routes, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚌 **").append(title).append("**\n\n");
        for (BusRoute r : routes) {
            sb.append("• **").append(r.getOrigin()).append(" → ").append(r.getDestination()).append("**\n");
            sb.append("  🕐 ").append(r.getDepartureTime()).append(" → ").append(r.getArrivalTime());
            if (r.getBusType() != null) sb.append("  |  ").append(r.getBusType().getLabel());
            sb.append("\n");
            sb.append("  💰 Rs. ").append(r.getPrice()).append("  |  🚌 ").append(r.getOperatorName());
            if (r.getBusBay() != null && !r.getBusBay().isBlank())
                sb.append("  |  🏁 Bay ").append(r.getBusBay());
            if (r.getContactNumber() != null && !r.getContactNumber().isBlank())
                sb.append("\n  📞 ").append(r.getContactNumber());
            sb.append("\n\n");
        }
        sb.append("👉 Search on the homepage to book your seat!");
        return sb.toString();
    }

    private String getAllRoutesInfo() {
        List<BusRoute> all = routeRepository.findByActiveTrue();
        Map<String, Long> counts = all.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getOrigin() + " → " + r.getDestination(),
                        LinkedHashMap::new, Collectors.counting()));

        StringBuilder sb = new StringBuilder("🚌 **All Available Routes**\n\n");
        counts.forEach((route, count) ->
                sb.append("• ").append(route).append(" (").append(count).append(" buses/day)\n"));
        sb.append("\n💡 Type a city or **'from X to Y'** to see schedules and prices.");
        return sb.toString();
    }

    private boolean matches(String input, String pattern) {
        return Pattern.compile(pattern).matcher(input).find();
    }
}