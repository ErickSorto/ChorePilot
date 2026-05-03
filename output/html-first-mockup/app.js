const imageBase = "./imagegen";

const L = config => ({ kind: "list", ...config });
const H = config => ({ kind: "hero", ...config });
const C = config => ({ kind: "chart", ...config });
const Y = config => ({ kind: "chart-calendar", ...config });
const D = config => ({ kind: "desktop", ...config });
const N = config => ({ kind: "notification", ...config });
const S = config => ({ kind: "settings", ...config });
const T = config => ({ kind: "states", ...config });
const G = (title, body, screens, eyebrow = "Screen group") => ({ title, body, screens, eyebrow });

const boards = [
  {
    step: "00",
    title: "Foundations",
    body:
      "Material 3 foundations translated into a warmer household system: token groups, typography, reusable shells, and the component language that carries the rest of the mockup.",
    tags: ["Material roles", "Inter type", "Reusable shell", "Adaptive logic"],
    groups: [
      G(
        "Theme foundation",
        "These reference cards anchor the light and dark role mapping, type scale, spacing cadence, and icon direction called for in the PDF.",
        [
          D({
            micro: "Light roles",
            step: "SYS1",
            title: "Material role map",
            copy:
              "Primary drives CTAs and selected states, secondary supports family coordination, and tertiary handles rewards, streaks, and premium accents.",
            preview: "foundation",
            cta: "Use light tokens",
            footer: "Primary, secondary, tertiary"
          }),
          D({
            micro: "Dark roles",
            step: "SYS2",
            title: "Dark token parity",
            copy:
              "Dark mode is a semantic swap, not a redesign. The same spacing, type, and layout logic carry over with softer surface contrast.",
            preview: "foundation",
            theme: "dark",
            cta: "Use dark tokens",
            footer: "Exact structural parity"
          }),
          L({
            micro: "Type and spacing",
            step: "SYS3",
            title: "Inter type scale",
            copy:
              "Display sizes are reserved for emotional onboarding and premium moments, while title and body roles keep the app surfaces calm and direct.",
            chips: ["Display 45/52", "Headline 32/40", "Body 16/24", "8pt rhythm"],
            items: [
              ["Section titles", "Headline Small or Title Large depending on surface density"],
              ["Card titles", "Title Medium for lists, reports, and settings groups"],
              ["Helper text", "Body Small or Label Small to preserve clarity in dense rows"]
            ],
            cta: "Lock typography",
            footer: "Google Fonts only"
          }),
          L({
            micro: "Iconography",
            step: "SYS4",
            title: "Rounded icon language",
            copy:
              "Material Symbols Rounded carry the product language so chores, proof, reports, and premium moments all read as one system.",
            chips: ["task_alt", "family_restroom", "verified", "analytics"],
            items: [
              ["Navigation", "Home, chores, family, reports, rewards, settings"],
              ["States", "Photo proof, success, warning, error, and streak markers"],
              ["Adaptive shell", "Drawer entries mirror the compact bottom navigation labels"]
            ],
            cta: "Use icon set",
            footer: "One family, one tone"
          })
        ],
        "System lane"
      ),
      G(
        "Shell and components",
        "The reusable shell pieces appear before the milestone pages so dashboard, chores, and admin flows all feel connected instead of screen-by-screen inventions.",
        [
          D({
            micro: "App shell",
            step: "SYS5",
            title: "Top app bar + bottom nav",
            copy:
              "Compact surfaces reuse a single branded app bar, nearby day carousel, and bottom navigation with stable labels and selected-state logic.",
            preview: "shell",
            cta: "Use compact shell",
            footer: "Shared on all core compact flows"
          }),
          D({
            micro: "Expanded shell",
            step: "SYS6",
            title: "Drawer + content + detail",
            copy:
              "Expanded layouts reuse one drawer system with calm summary cards and a supporting context pane instead of stretching the phone layout into empty space.",
            preview: "family",
            cta: "Use drawer shell",
            footer: "Reused across family, dashboard, reports"
          }),
          L({
            micro: "Core inventory",
            step: "SYS7",
            title: "Component inventory",
            copy:
              "Buttons, chips, cards, inputs, empty states, charts, sheets, and approval badges are called out explicitly so later boards stay grounded in the same patterns.",
            chips: ["Buttons", "Cards", "Inputs", "Badges", "Charts"],
            items: [
              ["Buttons", "Filled, outlined, tonal, text, FAB, icon, and destructive variants"],
              ["Inputs", "Outlined text fields, dropdowns, date/time pickers, switch rows, image slots"],
              ["Feedback", "Snackbar, dialog, bottom sheet, success banner, skeleton, empty, error"]
            ],
            cta: "Reference components",
            footer: "Matches the PDF inventory"
          }),
          N({
            micro: "System language",
            step: "SYS8",
            title: "Notification and state tone",
            copy:
              "Operational moments stay clear and family-safe: reminders sound direct, approvals feel calm, and empty states still preserve warmth.",
            notifications: [
              ["Kitchen reset", "Due at 6:00 PM with photo proof"],
              ["Emma completed bathroom wipe-down", "Proof approved by Mom"],
              ["No chores tonight", "Tomorrow is already balanced and ready"]
            ],
            cta: "Carry tone forward",
            footer: "Warm, not childish"
          })
        ],
        "Reusable systems"
      )
    ]
  },
  {
    step: "01",
    title: "01 Hero + Onboarding",
    body:
      "The full emotional onboarding lane moves from aspiration to reflection to a premium bridge, using the user's name, support art, and guided single-purpose screens instead of a generic setup wizard.",
    tags: ["13 required screens", "Optional review handoff", "Personalized copy", "Dark continuity"],
    groups: [
      G(
        "Light onboarding lane",
        "Every screen does one job. The early moments sell the future, the middle screens diagnose the household, and the final screens reward attention before introducing premium.",
        [
          H({
            micro: "Dream outcome",
            step: "1",
            title: "Dream outcome",
            copy:
              "Sell calmer evenings, smoother handoffs, and less parent mental load before the app asks for any setup work.",
            image: `${imageBase}/onboarding-hero-family-sync-v1.png`,
            chips: ["Calmer evenings", "Fewer reminders", "Visible teamwork"],
            cta: "See my calmer home",
            footer: "Aspirational opener"
          }),
          L({
            micro: "Name capture",
            step: "2",
            title: "What's your household name?",
            copy:
              "Personalization starts immediately so later screens can speak directly to Erick and the household he is setting up.",
            chips: ["One field only", "CTA stays disabled", "Soft support art"],
            items: [
              ["Family name", "The Sorto household"],
              ["Microcopy", "We will personalize reminders, reports, and encouragement"],
              ["Validation", "Continue unlocks after at least two characters"]
            ],
            cta: "Personalize my plan",
            footer: "Low friction pivot"
          }),
          L({
            micro: "Near-term goal",
            step: "3",
            title: "What would help your household first?",
            copy:
              "Single-column response rows keep the compact onboarding flow touch-friendly and calm.",
            chips: ["Fair chore sharing", "Fewer reminders", "Calmer evenings", "More independence"],
            items: [
              ["Fair chore sharing", "Reduce the daily question of who owns what"],
              ["Better accountability", "Make proof and completion visible without hovering"],
              ["Stronger routines", "Keep the week from falling apart when schedules shift"]
            ],
            cta: "Keep going",
            footer: "One question, one action"
          }),
          L({
            micro: "Identity outcome",
            step: "4",
            title: "What kind of home are you building?",
            copy:
              "The identity screen moves from practical friction into how the household wants to feel over time.",
            chips: ["Less parent mental load", "Fewer arguments", "Shared responsibility"],
            items: [
              ["Calmer home rhythm", "Household work feels predictable instead of reactive"],
              ["More responsibility in kids", "Progress is visible and easier to celebrate"],
              ["Stronger teamwork", "Everyone sees the same source of truth"]
            ],
            cta: "That fits us",
            footer: "Aspirational but grounded"
          }),
          H({
            micro: "Reflection",
            step: "5",
            title: "How heavy does the mental load feel right now?",
            copy:
              "This is the intentional pause screen: a reflective moment with one scale and responsive helper text instead of rushed setup.",
            image: `${imageBase}/onboarding-hero-family-sync-v1.png`,
            chips: ["5-step scale", "Reflective copy", "Stress-to-order art"],
            cta: "That feels accurate",
            footer: "Intentional friction"
          }),
          L({
            micro: "Household struggles",
            step: "6",
            title: "Where does the current routine break down?",
            copy:
              "Problem areas are explicit, non-judgmental, and visually polished so the screen feels designed rather than survey-like.",
            chips: ["Forgotten chores", "Uneven work", "No proof", "Busy-week drift"],
            items: [
              ["Chores are forgotten", "No one knows what still needs attention tonight"],
              ["Work feels uneven", "Parents carry the reminder and follow-up burden"],
              ["Proof is too fuzzy", "Adults still need to double-check what happened"]
            ],
            cta: "Continue",
            footer: "Specific and human"
          }),
          L({
            micro: "Feature excitement",
            step: "7",
            title: "What would make the biggest difference first?",
            copy:
              "The most desirable capability is framed through household relief, not through technical feature labels alone.",
            chips: ["Auto assignment", "Photo proof", "Weekly reports", "Vacation balancing"],
            items: [
              ["Auto assignment", "Keep chores moving without constant manual juggling"],
              ["Proof required", "Replace hovering with clear completion evidence"],
              ["Weekly reports", "Show fairness, consistency, and where help is needed"]
            ],
            cta: "Show me that",
            footer: "Benefit-led selection"
          }),
          L({
            micro: "Attribution",
            step: "8",
            title: "Where did you hear about ChorePilot?",
            copy:
              "This screen is intentionally lighter and shorter than the emotional screens around it so it does not break trust.",
            chips: ["TikTok", "Instagram", "Search", "Friend", "App Store"],
            items: [
              ["Visual treatment", "Branded chips and one short question only"],
              ["Optionality", "Other remains secondary instead of dominating the layout"],
              ["Trust", "No survey feeling and no forced justification"]
            ],
            cta: "Continue",
            footer: "Short and skimmable"
          }),
          H({
            micro: "Personalized snapshot",
            step: "9",
            title: "Erick, here's where your family can win first",
            copy:
              "The household snapshot is the first structured intelligence moment: insight cards, tags, and a reassuring sense that the system understands the household.",
            image: `${imageBase}/family-roster-management-v1.png`,
            chips: ["Biggest blocker", "Quick win", "Pattern summary", "First focus area"],
            cta: "This looks right",
            footer: "Structured value moment"
          }),
          L({
            micro: "Starting-point insight",
            step: "10",
            title: "Your starting point",
            copy:
              "Modeled insight is used instead of invented research claims, keeping the screen honest while still offering direction.",
            chips: ["Modeled insight", "No fake data", "Household-specific"],
            items: [
              ["Current strain", "Parents are carrying most of the reminder burden"],
              ["Most likely win", "Clear ownership and proof will lower daily friction fast"],
              ["Why it matters", "A visible routine makes fairness easier to protect"]
            ],
            cta: "Build this routine",
            footer: "Honest insight framing"
          }),
          L({
            micro: "7-day roadmap",
            step: "11",
            title: "Your first 7 days",
            copy:
              "A connected timeline helps the family picture what life feels like after Day 1, Day 3, and Day 7 instead of only listing setup tasks.",
            chips: ["Day 1 relief", "Day 3 momentum", "Day 7 calm"],
            items: [
              ["Day 1", "First responsibilities are visible and everyone knows tonight's plan"],
              ["Day 3", "Reminders drop and the home feels lighter at the end of the day"],
              ["Day 7", "Routine, fairness, and follow-through feel noticeably stronger"]
            ],
            cta: "Start week one",
            footer: "Outcome-led roadmap"
          }),
          L({
            micro: "Trust",
            step: "12",
            title: "Families stay because it lowers friction quickly",
            copy:
              "Trust cards and soft proof points reassure without inventing ratings or turning the layout into a loud testimonial collage.",
            chips: ["Clear direction", "Kids feel proud", "Parents stop chasing"],
            items: [
              ["Primary trust card", "The app feels useful before premium appears"],
              ["Quote rhythm", "One strong quote and two lighter support signals"],
              ["Forward motion", "The CTA carries momentum into the review or bridge step"]
            ],
            cta: "I can see it",
            footer: "High-trust, low-hype"
          }),
          L({
            micro: "Optional review",
            step: "12b",
            title: "How's this first preview feeling?",
            copy:
              "The review handoff is gentle, optional, and appears only after the user has already seen snapshot and trust value.",
            chips: ["Rate app", "Maybe later", "5-star row"],
            items: [
              ["Timing", "Only after the user feels understood"],
              ["Tone", "A calm ask, not a blocking wall"],
              ["Visual treatment", "A compact delight panel rather than a giant modal"]
            ],
            cta: "Rate app",
            footer: "Optional handoff"
          }),
          L({
            micro: "Premium bridge",
            step: "13",
            title: "Ready to unlock the deeper household system?",
            copy:
              "The bridge acknowledges the value already shown and introduces premium as leverage, not pressure.",
            chips: ["Advanced reports", "Balancing logic", "Approval workflows"],
            items: [
              ["Why now", "The family already sees what the core loop improves"],
              ["Bridge copy", "Promise the next layer without showing pricing yet"],
              ["Visual continuity", "Uses the premium art family and CTA tone"]
            ],
            cta: "See premium options",
            footer: "Handoff into page 02"
          })
        ],
        "Main lane"
      ),
      G(
        "Dark continuity",
        "A few dark-mode screens are shown in a dedicated region so the next implementation step can see parity without mixing themes in the main onboarding lane.",
        [
          H({
            micro: "Dark opener",
            step: "D1",
            title: "Dream outcome dark",
            copy:
              "The same emotional opener translated into dark surfaces with lower glare and the same CTA hierarchy.",
            image: `${imageBase}/onboarding-hero-family-sync-v1.png`,
            chips: ["Same layout", "Same copy roles", "Token swap only"],
            theme: "dark",
            cta: "Continue",
            footer: "Dark parity"
          }),
          H({
            micro: "Dark snapshot",
            step: "D2",
            title: "Snapshot dark",
            copy:
              "The structured insight moment keeps the same cards, labels, and order while shifting surfaces and text roles for dark mode.",
            image: `${imageBase}/family-roster-management-v1.png`,
            chips: ["Insight cards", "Reward chips", "Structured reassurance"],
            theme: "dark",
            cta: "Looks good",
            footer: "Same structure"
          }),
          L({
            micro: "Dark bridge",
            step: "D3",
            title: "Premium bridge dark",
            copy:
              "The bridge screen stays calm and premium in dark mode without turning into a new layout or separate emotional arc.",
            chips: ["Advanced reports", "Rules", "Approvals"],
            items: [
              ["Message", "The family is already seeing the value of clearer ownership"],
              ["Upgrade framing", "Premium deepens clarity and balancing, not the visual style"],
              ["CTA", "Carries the same promise into the premium page"]
            ],
            theme: "dark",
            cta: "Continue to premium",
            footer: "Separate dark lane"
          })
        ],
        "Dark lane"
      )
    ]
  },
  {
    step: "02",
    title: "02 Paywall + Premium Flow",
    body:
      "The premium page follows the required promise-to-offer sequence with a rescue branch, value explanation, and a slightly more energetic visual rhythm that still stays clean and trustworthy.",
    tags: ["4-screen main flow", "2-screen rescue branch", "Annual-first hierarchy", "Theme separation"],
    groups: [
      G(
        "Light premium sequence",
        "The light lane follows the exact flow from the PDF: Promise, Trial clarity, Social trust or unlocks, Offer sheet, then the separate abandonment rescue branch.",
        [
          H({
            micro: "Promise",
            step: "1",
            title: "Premium promise",
            copy:
              "Lead with calmer routines, clearer accountability, and less parent mental load before price appears anywhere on the page.",
            image: `${imageBase}/premium-hero-calm-home-v1.png`,
            chips: ["Erick, this is your calmer week", "Better fairness", "Deeper insights"],
            cta: "Show me premium",
            footer: "No price above the fold"
          }),
          L({
            micro: "Trial clarity",
            step: "2",
            title: "How the first week unfolds",
            copy:
              "A connected timeline explains what happens today, on Day 1, Day 3, and Day 7 without fake urgency or hidden billing logic.",
            chips: ["Today", "Day 1", "Day 3", "Day 7"],
            items: [
              ["Today", "Full premium access begins and the family sees the first upgraded surfaces"],
              ["Day 3", "Reminders drop, routines feel lighter, and fairness is easier to read"],
              ["Day 7", "Billing begins only if the configured trial truly ends here and the family stays in"]
            ],
            cta: "Start 7-day trial",
            footer: "Infographic-style clarity"
          }),
          L({
            micro: "What premium unlocks",
            step: "3",
            title: "What premium unlocks for your household",
            copy:
              "Because real review data is not assumed, this screen focuses on value explanation through family operations and reduced mental load.",
            chips: ["Advanced reports", "Smart rules", "Approvals", "Vacation balancing"],
            items: [
              ["Advanced reports", "Daily to yearly visibility across fairness, proof, and momentum"],
              ["Smart rules", "Assignment balancing changes when someone is gone or overloaded"],
              ["Approval workflows", "Proof review becomes clear instead of improvised"]
            ],
            cta: "I want that clarity",
            footer: "Value before price"
          }),
          L({
            micro: "Offer sheet",
            step: "4",
            title: "Choose the plan that keeps the rhythm going",
            copy:
              "The offer sheet is the first place where price appears, with annual preselected and the value difference explained plainly.",
            chips: ["Annual selected", "$59.99 per year", "$8.99 per month"],
            items: [
              ["Annual", "Best value for calmer routines, deeper reporting, and family accountability"],
              ["Monthly", "Secondary fallback for households that want shorter commitment"],
              ["Trust", "No fake slash pricing, no pretend weekly math, no false timers"]
            ],
            cta: "Unlock annual premium",
            footer: "The transactional step"
          }),
          H({
            micro: "Rescue gift",
            step: "R1",
            title: "A premium gift if you want a second look",
            copy:
              "The rescue branch feels like a distinct premium moment with curiosity and reward instead of desperation.",
            image: `${imageBase}/premium-hero-calm-home-v1.png`,
            chips: ["Special gift", "No price yet", "Curiosity-led"],
            cta: "Open the gift",
            footer: "Separate abandonment branch"
          }),
          L({
            micro: "Annual rescue offer",
            step: "R2",
            title: "One-time annual offer",
            copy:
              "The second rescue screen simplifies the choice: one annual plan, one CTA, one calm explanation of the household value.",
            chips: ["Annual only", "Simple CTA", "Truthful urgency only"],
            items: [
              ["Offer focus", "No monthly plan and no clutter competing with the main action"],
              ["Clarity", "The value is framed around calmer routines and easier coordination"],
              ["Separation", "This branch is visually distinct from the main offer sheet"]
            ],
            cta: "Claim annual offer",
            footer: "Cleaner rescue close"
          })
        ],
        "Main conversion lane"
      ),
      G(
        "Dark premium parity",
        "Dark mode is shown as its own region with the same offer hierarchy and rescue tone so the next design-to-code step does not invent a hybrid premium flow.",
        [
          H({
            micro: "Dark promise",
            step: "D1",
            title: "Premium promise dark",
            copy:
              "The opening premium hero keeps the same promise hierarchy while using softer dark surfaces and calmer contrast.",
            image: `${imageBase}/premium-hero-calm-home-v1.png`,
            chips: ["Same sequence", "Same pricing rule", "Same annual bias"],
            theme: "dark",
            cta: "Start premium",
            footer: "Dark lane"
          }),
          L({
            micro: "Dark offer sheet",
            step: "D2",
            title: "Offer sheet dark",
            copy:
              "The annual-first hierarchy remains intact while only the theme tokens and surfaces change.",
            chips: ["Annual selected", "Monthly secondary", "Trial clarity above"],
            items: [
              ["Annual", "The best-value choice remains the visual default"],
              ["Monthly", "Secondary fallback still exists when intended by metadata"],
              ["Trust", "No fake urgency or fake price math enters the dark version"]
            ],
            theme: "dark",
            cta: "Choose annual",
            footer: "Token swap only"
          })
        ],
        "Dark lane"
      )
    ]
  },
  {
    step: "03",
    title: "03 Milestone 1 - Core Experience",
    body:
      "Milestone 1 establishes the real daily product loop: auth support, dashboard, chores, detail, proof states, and a compact continuity reference back to onboarding.",
    tags: ["18 core screens", "Compact light + dark", "Proof states", "No dead-end empties"],
    groups: [
      G(
        "Milestone 1 system additions",
        "This page begins with the design-system pieces the first launch milestone adds: shell, components, and the illustration/states language used in the first production-ready flows.",
        [
          D({
            micro: "Milestone inventory",
            step: "M1-SYS1",
            title: "Shell and component intro",
            copy:
              "The first milestone introduces the reusable compact app shell, summary cards, day carousel, and the proof-oriented chore card.",
            preview: "shell",
            cta: "Review shell",
            footer: "Shared across dashboard and chores"
          }),
          L({
            micro: "Type + tokens",
            step: "M1-SYS2",
            title: "Light and dark token tables",
            copy:
              "Light and dark role groups, typography, spacing, and icon samples stay visible on the board so the implementation handoff has one stable reference area.",
            chips: ["Light tokens", "Dark tokens", "Typography", "Spacing", "Icons"],
            items: [
              ["Compact padding", "16 px outer padding with safe bounds respected"],
              ["Medium and expanded", "24-32 px outer padding with denser support panes"],
              ["Icon use", "Material Symbols Rounded on all launch flows"]
            ],
            cta: "Lock launch tokens",
            footer: "Stable visual reference"
          }),
          L({
            micro: "Core components",
            step: "M1-SYS3",
            title: "Launch component inventory",
            copy:
              "Buttons, chips, summary cards, day selectors, empty states, and permission prompts are called out before the main flow lane begins.",
            chips: ["Filled button", "Outlined field", "Summary card", "Day chip"],
            items: [
              ["Dashboard pieces", "Progress ring, summary strip, next-up card, momentum support card"],
              ["List pieces", "Assignee avatar, due time, proof badge, priority signal, complete action"],
              ["State pieces", "Success, approval pending, no chores, no proof queue"]
            ],
            cta: "See launch pieces",
            footer: "Used across all M1 screens"
          }),
          H({
            micro: "Illustration family",
            step: "M1-SYS4",
            title: "Launch empty states",
            copy:
              "The same warm illustration family supports the no-chore, no-proof, and quieter-end-of-day moments so the app never looks abandoned.",
            image: `${imageBase}/empty-state-no-chores-v1.png`,
            chips: ["Warm empty state", "Family-safe tone", "Reusable support art"],
            cta: "Use empty states",
            footer: "Shared support system"
          })
        ],
        "System additions"
      ),
      G(
        "Core flow lane",
        "The launch milestone screens follow the exact inventory in the brief, including both themes for the dashboard, today list, and chore detail flow.",
        [
          L({
            micro: "Splash light",
            step: "1",
            title: "ChorePilot",
            copy:
              "Keep home in sync with a gentle gradient opener, one tagline, and no stray implementation labels inside the frame.",
            chips: ["Light variant", "Tagline", "Open app"],
            items: [
              ["Tagline", "Keep home in sync"],
              ["Background", "Clean gradient with soft glow and calm focus"],
              ["Action", "One forward path into household entry"]
            ],
            cta: "Open app",
            footer: "Fast first impression"
          }),
          L({
            micro: "Splash dark",
            step: "2",
            title: "ChorePilot",
            copy:
              "The dark splash keeps the same brand lockup and spacing while shifting to the dark palette defined in the foundations page.",
            chips: ["Dark variant", "Same logo", "Same hierarchy"],
            items: [
              ["Tagline", "Keep home in sync"],
              ["Surface", "Deep navy background with soft indigo highlight"],
              ["Action", "Immediate handoff into auth or invite flow"]
            ],
            theme: "dark",
            cta: "Open app",
            footer: "Theme parity"
          }),
          L({
            micro: "Sign in",
            step: "3",
            title: "Welcome back",
            copy:
              "Sign-in supports the onboarding journey instead of replacing it, with one primary path and a softer create/join split below.",
            chips: ["Email", "Password", "Household-safe copy"],
            items: [
              ["Primary action", "Sign in and return to tonight's chore plan"],
              ["Secondary path", "Create or join a household if needed"],
              ["Trust note", "Private to your invited family"]
            ],
            cta: "Sign in",
            footer: "Auth supports the main journey"
          }),
          L({
            micro: "Create household",
            step: "4",
            title: "Create a new household",
            copy:
              "The creation path keeps the form lightweight while explaining how the household name will appear in reminders, reports, and invites.",
            chips: ["Household name", "Home rhythm", "Owner role"],
            items: [
              ["Household name", "The Sorto household"],
              ["Owner role", "Mom becomes the first admin and can invite others"],
              ["Preview", "The name appears in the first dashboard greeting"]
            ],
            cta: "Create household",
            footer: "One-screen creation"
          }),
          L({
            micro: "Join with code",
            step: "5",
            title: "Join an existing household",
            copy:
              "Invite-code entry is compact and reassuring, with a clear field, example format, and one obvious continue path.",
            chips: ["Invite code", "Email fallback", "Private join"],
            items: [
              ["Code field", "ABCD-4821"],
              ["Fallback", "Request a fresh invite from the household admin"],
              ["Result", "Your assigned chores appear the moment you join"]
            ],
            cta: "Join household",
            footer: "No confusion path"
          }),
          L({
            micro: "Notifications permission",
            step: "6",
            title: "Stay ahead of today's plan",
            copy:
              "The notification prompt explains value through concrete reminders, not generic permission copy.",
            chips: ["Due soon", "Proof reminder", "Streak alert"],
            items: [
              ["Next chore starts in 15 minutes", "Useful reminder copy shown in product language"],
              ["Kitchen cleanup is due at 6:00 PM", "Actionable and household-specific"],
              ["Timing note", "Permission is asked after value is visible"]
            ],
            cta: "Allow reminders",
            footer: "Value-led permission"
          }),
          L({
            micro: "Proof permission",
            step: "7",
            title: "Let proof stay clear and quick",
            copy:
              "Photo and proof access is framed around faster approval, fewer arguments, and a smoother family loop.",
            chips: ["Camera", "Photo library", "Admin approvals"],
            items: [
              ["Photo proof", "Snap a quick image for chores that need verification"],
              ["Short notes", "Add one sentence if a task needs extra context"],
              ["Result", "Parents review faster and the household keeps moving"]
            ],
            cta: "Enable proof",
            footer: "Purpose-driven permission"
          }),
          L({
            micro: "Dashboard light",
            step: "8",
            title: "Tonight's household rhythm",
            copy:
              "The command center pulls together progress ring, day carousel, next-up card, quick stats, and the overdue/upcoming list into one coherent story.",
            chips: ["Mon 29 highlighted", "4 of 7 complete", "Next up: Kitchen reset"],
            items: [
              ["Day carousel", "Yesterday, Today, Tue, Wed, Thu with Today emphasized"],
              ["Global summary", "7 chores, 4 completed, 2 pending, 1 overdue"],
              ["Momentum card", "Family streak is alive for 6 straight days"]
            ],
            cta: "View today's plan",
            footer: "Household command center"
          }),
          L({
            micro: "Dashboard dark",
            step: "9",
            title: "Tonight's household rhythm",
            copy:
              "The compact dark dashboard keeps the same order and support cards while swapping to darker surfaces and lighter text roles.",
            chips: ["Same carousel", "Same next-up card", "Dark tokens only"],
            items: [
              ["Progress ring", "4 of 7 complete with the same summary numbers"],
              ["Next-up card", "Kitchen reset is still due at 6:30 PM for Emma"],
              ["Quick stats", "Pending, overdue, and weekly momentum remain visible"]
            ],
            theme: "dark",
            cta: "Open today",
            footer: "Same structure"
          }),
          L({
            micro: "Today's chores light",
            step: "10",
            title: "Today's chores",
            copy:
              "The list screen groups active and completed chores while each card carries who, what, when, proof, and the completion action.",
            chips: ["Today / Upcoming / Completed", "Assignee avatar", "Proof chip"],
            items: [
              ["Unload dishwasher", "Mia · 6:00 PM · Photo proof · 12 min"],
              ["Take out trash", "Dad · 7:00 PM · Due soon · 8 min"],
              ["Feed the dog", "Noah · 6:15 PM · Quick tap · 5 min"]
            ],
            cta: "Mark a chore done",
            footer: "Real daily schedule"
          }),
          L({
            micro: "Today's chores dark",
            step: "11",
            title: "Today's chores",
            copy:
              "Dark mode preserves the grouping logic and task density, not just the same cards on a darker background.",
            chips: ["Schedule selector", "Priority indicator", "Bottom nav reused"],
            items: [
              ["Clean kitchen table", "Emma · Due 6:45 PM · Camera proof"],
              ["Sweep living room", "Mom · 7:15 PM · Due later · 15 min"],
              ["Completed", "Fold laundry · Dad · Approved"]
            ],
            theme: "dark",
            cta: "Keep moving",
            footer: "Dark continuity"
          }),
          L({
            micro: "Chore detail light",
            step: "12",
            title: "Kitchen reset",
            copy:
              "The detail screen names the task, assigned person, notes, proof rule, subtasks, and the complete or upload-proof actions without looking like a blank form.",
            chips: ["Emma", "Photo proof", "6:30 PM", "12 min"],
            items: [
              ["Description", "Wipe counters, load stray dishes, and reset the table"],
              ["Subtasks", "Counters, dishwasher check, floor scan"],
              ["Actions", "Complete now or upload proof if the parent requires it"]
            ],
            cta: "Upload proof",
            footer: "Specific and actionable"
          }),
          L({
            micro: "Chore detail dark",
            step: "13",
            title: "Kitchen reset",
            copy:
              "The dark variant keeps the same field order and action structure so it can be implemented as a token swap instead of a new screen.",
            chips: ["Same subtasks", "Same proof rule", "Dark surfaces"],
            items: [
              ["Assigned person", "Emma"],
              ["Suggested time", "6:30 PM after dinner"],
              ["Notes", "Take one clear photo if the counters were cluttered"]
            ],
            theme: "dark",
            cta: "Complete chore",
            footer: "Exact parity"
          }),
          H({
            micro: "Success state",
            step: "14",
            title: "Nice work, Emma",
            copy:
              "Success feels rewarding but still calm, with one celebration note and the next useful action instead of a loud gamified interruption.",
            image: `${imageBase}/empty-state-no-chores-v1.png`,
            chips: ["Success banner", "Streak note", "Next chore preview"],
            cta: "See what's next",
            footer: "Warm completion moment"
          }),
          L({
            micro: "Awaiting approval",
            step: "15",
            title: "Proof sent for review",
            copy:
              "The post-upload state explains exactly what happens next so members are not left guessing whether a chore counts yet.",
            chips: ["Photo uploaded", "Admin review", "Pending badge"],
            items: [
              ["Status", "Awaiting approval from Mom"],
              ["Proof note", "One image and an optional note are attached"],
              ["What happens next", "The member sees approved or redo feedback in the activity feed"]
            ],
            cta: "Back to chores",
            footer: "Clear next state"
          }),
          H({
            micro: "No chores today",
            step: "16",
            title: "No chores tonight",
            copy:
              "The empty state still feels useful by celebrating the calm evening and previewing tomorrow's first assignment.",
            image: `${imageBase}/empty-state-no-chores-v1.png`,
            chips: ["Tomorrow preview", "Keep streak warm", "No blank dead end"],
            cta: "Enjoy tonight",
            footer: "Warm empty state"
          }),
          H({
            micro: "No proof pending",
            step: "17",
            title: "No proof submissions waiting",
            copy:
              "Proof review empties reassure the admin that the household is caught up and show where new submissions will appear later.",
            image: `${imageBase}/family-roster-management-v1.png`,
            chips: ["Review queue clear", "Approvals caught up", "Next proof arrives here"],
            cta: "Return to dashboard",
            footer: "Admin-ready empty"
          }),
          H({
            micro: "Continuity reference",
            step: "18",
            title: "Onboarding continuity thumbnail",
            copy:
              "Milestone 1 only keeps a compact reference to the standalone onboarding page so the board stays focused on the first launch loop.",
            image: `${imageBase}/onboarding-hero-family-sync-v1.png`,
            chips: ["Standalone page 01", "Compact reference only", "Narrative continuity"],
            cta: "Open onboarding lane",
            footer: "Reference, not duplicate"
          })
        ],
        "Product flow"
      )
    ]
  },
  {
    step: "04",
    title: "04 Milestone 2 - Family + Chore Management",
    body:
      "Milestone 2 expands the product into role-aware family management, full chore creation, proof review, template picking, and availability modes that rebalance the household automatically.",
    tags: ["21 management screens", "Admin and member views", "Proof workflow", "Expanded family shell"],
    groups: [
      G(
        "Milestone 2 system additions",
        "The management milestone introduces richer inputs, approval badges, image upload patterns, and a reusable expanded drawer shell before the full management flows begin.",
        [
          L({
            micro: "New inputs",
            step: "M2-SYS1",
            title: "Management controls",
            copy:
              "Segmented controls, switches, date and time pickers, dropdowns, image slots, and role badges are all visible before the form-heavy screens appear.",
            chips: ["Segmented buttons", "Switch rows", "Date picker", "Dropdown", "Upload card"],
            items: [
              ["Assignment controls", "Assignee selection and auto-assign toggles work together"],
              ["Proof controls", "Photo, video, note, or none with clear icon rows"],
              ["Rules controls", "Days of week and reminder settings remain readable at compact widths"]
            ],
            cta: "Use management controls",
            footer: "System before screens"
          }),
          D({
            micro: "Drawer shell",
            step: "M2-SYS2",
            title: "Expanded family shell",
            copy:
              "The family and admin expanded layouts reuse the same drawer and supporting-pane logic instead of inventing separate desktop products.",
            preview: "family",
            cta: "Use drawer layout",
            footer: "Family, admin, and profile views"
          }),
          L({
            micro: "Proof atoms",
            step: "M2-SYS3",
            title: "Approval badges and proof cards",
            copy:
              "Pending, approved, needs redo, and upload-ready states are all specified before the proof queue and detail pane use them at full scale.",
            chips: ["Pending", "Approved", "Needs redo", "Upload ready"],
            items: [
              ["Queue row", "Title, member avatar, proof chip, status badge, preview"],
              ["Detail pane", "Large proof preview, note card, admin comment, decision buttons"],
              ["History language", "Status terms stay explicit and kid-friendly"]
            ],
            cta: "Use proof states",
            footer: "Consistent moderation language"
          })
        ],
        "System additions"
      ),
      G(
        "Family and management screens",
        "These are the exact Milestone 2 screens from the brief, grouped so compact, expanded, admin, and edge-state surfaces are easy to scan before Figma capture.",
        [
          H({
            micro: "Family overview light",
            step: "1",
            title: "The Sorto household",
            copy:
              "The compact family overview acts like a real household management screen with avatars, role badges, fairness summary, and an invite action.",
            image: `${imageBase}/family-roster-management-v1.png`,
            chips: ["Role badges", "Invite member", "Fairness summary"],
            cta: "Invite member",
            footer: "Compact control center"
          }),
          H({
            micro: "Family overview dark",
            step: "2",
            title: "The Sorto household",
            copy:
              "The dark variant keeps the same roster, summary, and quick-action structure while shifting to the dark role tokens.",
            image: `${imageBase}/family-roster-management-v1.png`,
            chips: ["Same roster", "Same summary", "Dark lane"],
            theme: "dark",
            cta: "Manage family",
            footer: "Theme parity"
          }),
          D({
            micro: "Expanded family light",
            step: "3",
            title: "Expanded family overview",
            copy:
              "The expanded overview uses the reusable drawer, member list, summary pane, and a support panel for fairness or streak context.",
            preview: "family",
            cta: "Review family layout",
            footer: "Drawer + content + support"
          }),
          D({
            micro: "Expanded family dark",
            step: "4",
            title: "Expanded family overview",
            copy:
              "The dark expanded layout mirrors the light version exactly so it can be implemented as a token swap rather than a separate composition.",
            preview: "family",
            theme: "dark",
            cta: "Review dark layout",
            footer: "Same structure"
          }),
          L({
            micro: "Member profile compact",
            step: "5",
            title: "Emma's profile",
            copy:
              "The compact profile shows role, streak, assigned chore load, completion rate, and availability modes in one clear vertical stack.",
            chips: ["Member badge", "Completion rate", "Streak", "Availability"],
            items: [
              ["This week", "9 assigned · 8 completed · 1 pending"],
              ["Favorite category", "Kitchen and pet care"],
              ["Availability", "Gone Friday evening and back Saturday morning"]
            ],
            cta: "Edit member",
            footer: "Specific member view"
          }),
          D({
            micro: "Member profile expanded",
            step: "6",
            title: "Expanded profile detail",
            copy:
              "The expanded profile adds a detail pane with history, fairness context, and next-step admin actions without changing the core information architecture.",
            preview: "profile",
            cta: "Inspect detail view",
            footer: "List-detail adaptation"
          }),
          D({
            micro: "Admin dashboard",
            step: "7",
            title: "Admin dashboard variant",
            copy:
              "Admin mode emphasizes assignment health, proof queue, and rules entry points while preserving the household tone of the main product.",
            preview: "dashboard",
            cta: "Open admin view",
            footer: "Admin-specific surface"
          }),
          L({
            micro: "Member dashboard",
            step: "8",
            title: "Member dashboard variant",
            copy:
              "The member version stays focused on assigned chores, progress, and personal availability controls instead of household-wide actions.",
            chips: ["Assigned today", "Proof reminders", "Personal streak"],
            items: [
              ["Assigned to Mia", "Unload dishwasher and feed the dog tonight"],
              ["Progress", "2 of 3 assigned chores completed"],
              ["Availability", "Day off tomorrow is still editable"]
            ],
            cta: "View my chores",
            footer: "Member-focused view"
          }),
          L({
            micro: "Create chore compact",
            step: "9",
            title: "Create chore",
            copy:
              "The compact form groups Basics, Assignment, Timing, Proof, Rewards, and Rules so the flow feels dense but understandable.",
            chips: ["Kitchen reset", "Emma", "Mon / Wed / Fri", "Photo proof"],
            items: [
              ["Basics", "Name, description, category, difficulty, and points"],
              ["Timing", "Preferred time, due time, estimated duration, repeat days"],
              ["Rules", "Auto-assign, admin approval, reminders, late policy"]
            ],
            cta: "Publish chore",
            footer: "Premium but practical"
          }),
          D({
            micro: "Create chore expanded",
            step: "10",
            title: "Create chore expanded",
            copy:
              "Expanded creation uses a wider form pane plus a live summary card so the extra width adds context instead of empty margins.",
            preview: "form",
            cta: "Review expanded form",
            footer: "Wide form + live summary"
          }),
          L({
            micro: "Edit chore compact",
            step: "11",
            title: "Edit chore",
            copy:
              "The edit view keeps the same structure as create while adding the current schedule, assignee history, and recent proof expectations.",
            chips: ["Existing schedule", "Last edited by Mom", "Reward value"],
            items: [
              ["Current assignee", "Emma with auto-rebalance turned on"],
              ["Proof type", "Photo required when the kitchen was messy"],
              ["Reminder", "6:15 PM with due time at 6:45 PM"]
            ],
            cta: "Save updates",
            footer: "Edit parity"
          }),
          L({
            micro: "Template picker",
            step: "12",
            title: "Global chore library",
            copy:
              "The template picker feels helpful and premium with searchable starter tasks, category filters, and one clear use-template action on every row.",
            chips: ["Kitchen", "Bathroom", "Laundry", "Pets", "Shared spaces"],
            items: [
              ["Load dishwasher", "After dinner kitchen reset · 10 min"],
              ["Bathroom wipe-down", "Mirror, sink, and quick floor check · 12 min"],
              ["Dog feeding", "Evening bowl and water refresh · 5 min"]
            ],
            cta: "Use template",
            footer: "Helpful from the first tap"
          }),
          L({
            micro: "Assign rules",
            step: "13",
            title: "Assignment rules",
            copy:
              "Rule configuration makes balancing logic explicit with auto-assign, member eligibility, and fairness constraints.",
            chips: ["Auto-balance", "Skip unavailable", "Keep fairness"],
            items: [
              ["Eligible members", "Mom, Dad, Emma, Noah, Mia"],
              ["When someone is gone", "Shift urgent chores to available members first"],
              ["Fairness guardrail", "Do not give one member more than two heavy chores in a row"]
            ],
            cta: "Save rules",
            footer: "Explainable automation"
          }),
          L({
            micro: "Proof queue",
            step: "14",
            title: "Proof review queue",
            copy:
              "Queue rows include title, member, proof type, status, and preview so admins can triage quickly without opening every submission.",
            chips: ["Photo", "Video", "Note", "Pending"],
            items: [
              ["Bathroom wipe-down", "Emma · Photo proof · Submitted 12m ago"],
              ["Dog feeding", "Noah · Note proof · Submitted 5m ago"],
              ["Trash duty", "Dad · Photo proof · Needs quick review"]
            ],
            cta: "Review queue",
            footer: "Meaningful moderation list"
          }),
          D({
            micro: "Proof detail pane",
            step: "15",
            title: "Proof detail review",
            copy:
              "The detail pane keeps the uploaded preview, member note, task reminder, comment field, and decision buttons on one screen.",
            preview: "detail",
            cta: "Approve or redo",
            footer: "Context-rich decision pane"
          }),
          L({
            micro: "Vacation mode",
            step: "16",
            title: "Vacation mode",
            copy:
              "Vacation mode shows the start and end dates plus a rebalance summary so the household understands what changes immediately.",
            chips: ["Unavailable Jul 2-8", "Rebalance preview", "Smart shift"],
            items: [
              ["Who is away", "Mom is out of rotation for one week"],
              ["What changes", "Kitchen cleanup moves to Dad and Emma on alternating nights"],
              ["How it returns", "The normal plan resumes automatically on Jul 9"]
            ],
            cta: "Activate vacation mode",
            footer: "Clear schedule impact"
          }),
          L({
            micro: "Day off mode",
            step: "17",
            title: "Day off",
            copy:
              "A single-day mode feels lighter than vacation but still previews how the workload shifts for that one date.",
            chips: ["One-day pause", "Tomorrow only", "Quick rebalance"],
            items: [
              ["Unavailable person", "Emma is off tomorrow after school"],
              ["Shifted chores", "Feed the dog moves to Noah and dishwasher goes to Dad"],
              ["Support copy", "The app returns to the regular plan automatically the next day"]
            ],
            cta: "Save day off",
            footer: "Lightweight exception flow"
          }),
          L({
            micro: "Gone mode",
            step: "18",
            title: "Gone mode",
            copy:
              "Gone mode handles unpredictable travel or absence with clear helper text and a visible before-and-after assignment preview.",
            chips: ["Travel", "Unknown end date", "Priority chores first"],
            items: [
              ["Status", "Dad is away until further notice"],
              ["Priority logic", "Urgent chores shift first, lower-priority tasks may pause"],
              ["Admin note", "The family can still manually override the rebalance"]
            ],
            cta: "Turn on gone mode",
            footer: "Flexible absence logic"
          }),
          S({
            micro: "Notification preferences",
            step: "19",
            title: "Notification preferences",
            copy:
              "Members and admins can tune reminders, proof nudges, and streak updates without wandering through a generic settings maze.",
            settings: [
              ["Due soon reminders", "Notify 15 minutes before the scheduled time", true],
              ["Proof-needed alerts", "Show a second reminder for proof-required chores", true],
              ["Quiet hours", "Silence non-urgent household nudges after 9:30 PM", false]
            ],
            cta: "Save preferences",
            footer: "Role-aware settings"
          }),
          L({
            micro: "Invite member",
            step: "20",
            title: "Invite a member",
            copy:
              "The invite flow explains the role, the code or link, and the first thing the new member will see after joining.",
            chips: ["Invite code", "Copy link", "Choose role"],
            items: [
              ["Role preview", "Member, parent admin, or temporary helper"],
              ["Invite format", "ABCD-4821 or a one-tap household link"],
              ["After join", "The member lands in their assigned dashboard view"]
            ],
            cta: "Send invite",
            footer: "Clear membership handoff"
          }),
          H({
            micro: "Family empty state",
            step: "21",
            title: "Your household is ready for its first member",
            copy:
              "The empty state still feels like a living family control center, with a clear invite action and a preview of how roles and progress will appear.",
            image: `${imageBase}/family-roster-management-v1.png`,
            chips: ["Invite the first member", "Role explanation", "No dead space"],
            cta: "Invite now",
            footer: "Management empty state"
          })
        ],
        "Product flow"
      )
    ]
  },
  {
    step: "05",
    title: "05 Milestone 3 - Reports + Premium",
    body:
      "Milestone 3 introduces the full reporting stack, streaks and rewards, premium locks, and the continuity surfaces that point back into the dedicated premium conversion page.",
    tags: ["15 report screens", "Daily to yearly", "Premium lock states", "Rewards and continuity"],
    groups: [
      G(
        "Milestone 3 system additions",
        "Before the report pages begin, the board calls out the chart types, premium badges, pricing cards, and blurred locked states introduced in the reporting milestone.",
        [
          L({
            micro: "Charts",
            step: "M3-SYS1",
            title: "Report chart kit",
            copy:
              "Weekly bars, monthly trends, donut splits, comparison tables, and yearly heatmaps are all labeled so the charts feel meaningful, not ornamental.",
            chips: ["Bar", "Line", "Donut", "Comparison", "Heatmap"],
            items: [
              ["Chart rule", "Every chart names the metric, time range, and why it matters"],
              ["Family compare", "Stacked bars are used for contribution and fairness context"],
              ["Heatmap", "Yearly completion texture appears only with a legend and summary note"]
            ],
            cta: "Use chart kit",
            footer: "Label every metric"
          }),
          L({
            micro: "Premium surfaces",
            step: "M3-SYS2",
            title: "Locked and pricing surfaces",
            copy:
              "Premium report cards, locked states, and compact pricing cues stay visible on the board so the product never falls back to a vague generic paywall panel.",
            chips: ["Premium badge", "Blurred lock", "Pricing card", "Value note"],
            items: [
              ["Locked state", "Keep the report visible enough to explain the value"],
              ["CTA framing", "Unlock yearly insights and balancing logic"],
              ["Continuity", "Compact premium thumbnail points back to page 02"]
            ],
            cta: "Use premium surfaces",
            footer: "Contextual upgrade cues"
          }),
          H({
            micro: "Rewards support",
            step: "M3-SYS3",
            title: "Reward and celebration support art",
            copy:
              "Rewards stay warm and motivating, using the same art family as onboarding and premium instead of feeling like a disconnected gamification layer.",
            image: `${imageBase}/premium-hero-calm-home-v1.png`,
            chips: ["Celebration", "Streak warmth", "Premium continuity"],
            cta: "Use reward art",
            footer: "Same illustration family"
          })
        ],
        "System additions"
      ),
      G(
        "Reports and premium contexts",
        "The report lane covers all four report levels, comparison, streaks, locks, subscription entry, and the empty states that keep the milestone from feeling sparse.",
        [
          C({
            micro: "Daily report light",
            step: "1",
            title: "Daily report",
            copy:
              "Assigned today, completed today, pending, overdue, completion percentage, and per-member contribution all appear on the same compact surface.",
            chips: ["Assigned 7", "Completed 4", "Pending 2", "Overdue 1"],
            bars: [24, 48, 64, 56],
            cta: "Open daily report",
            footer: "Today's accountability"
          }),
          C({
            micro: "Daily report dark",
            step: "2",
            title: "Daily report",
            copy:
              "The dark report uses the same chart ordering and labels so only the semantic tokens change between themes.",
            chips: ["Same metrics", "Same layout", "Dark surfaces"],
            bars: [24, 48, 64, 56],
            theme: "dark",
            cta: "Review daily report",
            footer: "Dark parity"
          }),
          C({
            micro: "Weekly report light",
            step: "3",
            title: "Weekly report",
            copy:
              "This screen tracks completions by day, most consistent member, proof rate, and streak change versus last week.",
            chips: ["By day", "Most consistent", "Proof rate", "Streak change"],
            bars: [18, 28, 42, 56, 66, 58, 44],
            altBars: true,
            cta: "See weekly rhythm",
            footer: "Family cadence"
          }),
          C({
            micro: "Weekly report dark",
            step: "4",
            title: "Weekly report",
            copy:
              "Dark mode preserves the same weekly breakdown, keeping the report implementation predictable across themes.",
            chips: ["Same chart", "Same labels", "Dark surfaces"],
            bars: [18, 28, 42, 56, 66, 58, 44],
            altBars: true,
            theme: "dark",
            cta: "See weekly report",
            footer: "Dark continuity"
          }),
          C({
            micro: "Monthly report",
            step: "5",
            title: "Monthly report",
            copy:
              "Monthly trends combine category breakdown, skipped or reassigned chores, per-member totals, and comparison against the previous month.",
            chips: ["Trend line", "Category split", "Per-member totals"],
            bars: [22, 36, 42, 58, 64, 72],
            cta: "Explore month",
            footer: "Broader operating view"
          }),
          Y({
            micro: "Yearly report",
            step: "6",
            title: "Yearly report",
            copy:
              "The yearly surface zooms out into consistency trend, strongest and weakest months, total chores, family contribution, and a heatmap calendar.",
            chips: ["Heatmap", "Strongest month", "Weakest month", "Annual milestones"],
            bars: [14, 24, 36, 44, 58, 72],
            cta: "Open yearly view",
            footer: "Long-range consistency"
          }),
          D({
            micro: "Expanded weekly report",
            step: "7",
            title: "Weekly report with detail pane",
            copy:
              "Expanded reporting uses the reusable wide analytics shell so filters, chart detail, and summary notes all earn the extra width.",
            preview: "reports",
            cta: "Inspect expanded weekly",
            footer: "Wide analytic adaptation"
          }),
          C({
            micro: "Household comparison",
            step: "8",
            title: "Household comparison report",
            copy:
              "Contribution comparison makes fairness visible across family members without turning the app into a corporate dashboard.",
            chips: ["Contribution", "Fairness", "Support needed"],
            bars: [34, 44, 52, 38, 26],
            altBars: true,
            cta: "Compare family",
            footer: "Fairness context"
          }),
          L({
            micro: "Streaks and rewards",
            step: "9",
            title: "Streaks and rewards",
            copy:
              "Rewards celebrate consistency through streak badges, milestone cards, and small family-safe moments instead of flashy game mechanics.",
            chips: ["7-day streak", "Weekend clean sweep", "Kitchen streak"],
            items: [
              ["Emma", "Reached a 7-day completion streak with two photo-approved chores"],
              ["Household", "Three straight evenings with zero overdue tasks"],
              ["Next reward", "Movie-night token unlocks after two more clean days"]
            ],
            cta: "View rewards",
            footer: "Motivation without noise"
          }),
          L({
            micro: "Locked report",
            step: "10",
            title: "Locked premium report",
            copy:
              "The locked report state keeps enough of the chart visible to explain the upgrade value while still respecting the premium boundary.",
            chips: ["Blurred details", "Premium badge", "Unlock yearly"],
            items: [
              ["Visible value", "Trend summary, yearly depth, and family compare are teased clearly"],
              ["Upgrade copy", "Unlock deeper household clarity and fairness tracking"],
              ["Context", "The free tier remains useful for basic daily and weekly views"]
            ],
            cta: "Unlock yearly insights",
            footer: "Contextual lock"
          }),
          L({
            micro: "Locked automation",
            step: "11",
            title: "Locked premium automation",
            copy:
              "Automation locks stay close to the workflow they would improve so the user understands what they gain after upgrading.",
            chips: ["Smart rules", "Vacation balancing", "Reminder logic"],
            items: [
              ["Free tier", "Basic reminders and manual assignment remain available"],
              ["Premium tier", "Balancing logic and advanced reminder branches unlock"],
              ["Why it matters", "Less parent juggling during schedule changes"]
            ],
            cta: "Unlock smart rules",
            footer: "Specific premium story"
          }),
          L({
            micro: "Subscription entry",
            step: "12",
            title: "Subscription management entry",
            copy:
              "Subscription entry sits inside settings and reports with clear ownership, plan status, and a direct path to the dedicated premium flow page.",
            chips: ["Current plan", "Renewal date", "Household owner only"],
            items: [
              ["Plan", "Family Premium Annual"],
              ["Owner", "Mom manages billing and renewal"],
              ["Actions", "Change plan, manage renewal, view premium benefits"]
            ],
            cta: "Manage plan",
            footer: "Operational billing entry"
          }),
          H({
            micro: "Premium continuity",
            step: "13",
            title: "Premium flow continuity thumbnail",
            copy:
              "Milestone 3 references the standalone premium flow page without duplicating the entire conversion lane at full size.",
            image: `${imageBase}/premium-hero-calm-home-v1.png`,
            chips: ["Standalone page 02", "Compact reference", "Clear continuity"],
            cta: "Open premium page",
            footer: "Reference only"
          }),
          H({
            micro: "Report empty state",
            step: "14",
            title: "Your first report will show up after tonight's chores",
            copy:
              "The empty report state explains what data will appear later and invites the household to finish the first routine rather than leaving a blank chart container.",
            image: `${imageBase}/empty-state-no-chores-v1.png`,
            chips: ["No blank graph", "What appears later", "Warm expectation"],
            cta: "Back to dashboard",
            footer: "Useful empty state"
          }),
          H({
            micro: "No data yet",
            step: "15",
            title: "Not enough history yet",
            copy:
              "The no-data state keeps the report hierarchy visible while explaining that weekly and yearly insights need a little more household history.",
            image: `${imageBase}/empty-state-no-chores-v1.png`,
            chips: ["Needs history", "What unlocks next", "No dead-end copy"],
            cta: "Keep the streak going",
            footer: "Progressive data state"
          })
        ],
        "Product flow"
      )
    ]
  },
  {
    step: "06",
    title: "06 Milestone 4 - Final Adaptive + Platform States",
    body:
      "The final milestone proves the product can scale into platform notifications, adaptive desktop boards, billing and settings surfaces, and branded resilient states without losing its identity.",
    tags: ["20 final screens", "Notifications", "Expanded adaptive boards", "Settings and resiliency"],
    groups: [
      G(
        "Milestone 4 system additions",
        "The final milestone introduces notification layouts, dialogs and sheets, keyboard-aware patterns, and the remaining system-state surfaces before the adaptive boards appear.",
        [
          L({
            micro: "System surfaces",
            step: "M4-SYS1",
            title: "Snackbars, dialogs, sheets",
            copy:
              "Transient feedback surfaces stay concise and calm so admin actions and completion states feel polished without turning playful or noisy.",
            chips: ["Snackbar", "Dialog", "Bottom sheet", "Success banner"],
            items: [
              ["Snackbar", "Undo missed swipe or confirm a role change"],
              ["Dialog", "Confirm premium or destructive admin actions with clear text labels"],
              ["Bottom sheet", "Use for adaptive pickers and mobile detail expansion"]
            ],
            cta: "Use final surfaces",
            footer: "Last system additions"
          }),
          L({
            micro: "Keyboard-aware",
            step: "M4-SYS2",
            title: "Keyboard and wider data layouts",
            copy:
              "Longer settings and billing forms account for the keyboard, while wider layouts use support panes instead of giant empty cards.",
            chips: ["Keyboard inset", "Sticky action", "Support pane"],
            items: [
              ["Compact forms", "Sticky CTA and scroll-safe inputs stay above the keyboard"],
              ["Wide forms", "Billing summary and help live in the support pane"],
              ["Guardrail", "No giant empty gutters on tablet or desktop"]
            ],
            cta: "Apply layout rules",
            footer: "Adaptive quality guardrail"
          }),
          N({
            micro: "Platform language",
            step: "M4-SYS3",
            title: "Notification content language",
            copy:
              "Android and iOS notifications reuse the same task-first language while adopting the correct platform-specific hierarchy and action treatment.",
            notifications: [
              ["Next chore starts in 15 minutes", "Mark done · Snooze · View"],
              ["Kitchen cleanup is due at 6:00 PM", "Photo proof required tonight"],
              ["You're on a 7-day streak", "Open the family momentum report"]
            ],
            cta: "Review platform tone",
            footer: "Shared copy, platform-native chrome"
          }),
          T({
            micro: "Resiliency",
            step: "M4-SYS4",
            title: "Offline, error, loading language",
            copy:
              "The broken moments are still branded and explicit, with clear guidance on what happened and what the family can do next.",
            cta: "Use fallback states",
            footer: "No generic failures"
          })
        ],
        "System additions"
      ),
      G(
        "Adaptive and platform screens",
        "The final lane covers notification center, native notifications, expanded product boards, settings, billing, and the last polished system states called for in the brief.",
        [
          N({
            micro: "Notification center light",
            step: "1",
            title: "Notification center",
            copy:
              "The compact center gathers reminders, approvals, streak notes, and system updates into one clear list-detail-ready surface.",
            notifications: [
              ["Kitchen cleanup", "Due at 6:00 PM · Emma · Photo proof needed"],
              ["Bathroom wipe-down", "Approved 12 minutes ago by Mom"],
              ["7-day streak", "The family has kept momentum all week"]
            ],
            cta: "Review notifications",
            footer: "Compact inbox surface"
          }),
          N({
            micro: "Notification center dark",
            step: "2",
            title: "Notification center",
            copy:
              "Dark mode keeps the same notification hierarchy and list density so the experience stays predictable after sunset.",
            notifications: [
              ["Trash duty", "Proof required tonight at 7:00 PM"],
              ["Laundry moved", "Completed by Dad and logged to the timeline"],
              ["Tomorrow's first chore", "Dog feeding is already assigned"]
            ],
            theme: "dark",
            cta: "Open center",
            footer: "Dark parity"
          }),
          N({
            micro: "Android collapsed",
            step: "3",
            title: "Android notification collapsed",
            copy:
              "The collapsed Android notification shows the app icon, chore title, assignee label, due time, proof badge, and quick actions at a glance.",
            notifications: [
              ["Kitchen cleanup", "You · Due 6:00 PM · Mark Done · Snooze · View"],
              ["Proof required", "Trash duty still needs a quick photo"],
              ["Weekly report ready", "Open summary"]
            ],
            cta: "Review Android collapsed",
            footer: "Compact action strip"
          }),
          N({
            micro: "Android expanded",
            step: "4",
            title: "Android notification expanded",
            copy:
              "Expanded Android surfaces reveal more proof context and action chips without breaking the task-first hierarchy.",
            notifications: [
              ["Bathroom cleaning", "Emma · Due 6:30 PM · Photo proof required · Mark Done"],
              ["Kitchen reset", "Quick view, snooze, or jump into the detail screen"],
              ["Assignment rebalance", "Vacation mode shifted two chores tonight"]
            ],
            cta: "Review Android expanded",
            footer: "Expanded quick actions"
          }),
          N({
            micro: "iOS preview",
            step: "5",
            title: "iOS notification preview",
            copy:
              "The iOS version keeps the same chore language with softer grouping and the lighter action treatment expected on the platform.",
            notifications: [
              ["Kitchen cleanup is due at 6:00 PM", "Proof required for Trash Duty"],
              ["Emma completed Bathroom Cleaning", "Open the review panel"],
              ["You're on a 7-day streak", "Celebrate and keep the rhythm going"]
            ],
            cta: "Review iOS preview",
            footer: "Platform-native hierarchy"
          }),
          N({
            micro: "Persistent reminder",
            step: "6",
            title: "Persistent reminder concept",
            copy:
              "A service-style reminder stays useful and polite, helping the household glance at the next assignment without feeling nagged.",
            notifications: [
              ["Next chore starts in 15 minutes", "Kitchen reset · Emma · View"],
              ["Overdue task", "Take out trash is still open · Snooze"],
              ["Proof queue", "1 submission needs review"]
            ],
            cta: "Review reminder concept",
            footer: "Only if the platform allows it"
          }),
          D({
            micro: "Expanded dashboard light",
            step: "7",
            title: "Expanded dashboard",
            copy:
              "The wide dashboard uses the reusable drawer, summary cards, central assignment board, and right-side context pane instead of stretching the phone view.",
            preview: "dashboard",
            cta: "Open expanded dashboard",
            footer: "Adaptive dashboard"
          }),
          D({
            micro: "Expanded dashboard dark",
            step: "8",
            title: "Expanded dashboard",
            copy:
              "The dark wide dashboard mirrors the light layout exactly, keeping the nav, summary, board, and context pane in the same order.",
            preview: "dashboard",
            theme: "dark",
            cta: "Open dark dashboard",
            footer: "Same adaptive shell"
          }),
          D({
            micro: "Expanded chore board light",
            step: "9",
            title: "Expanded chore board",
            copy:
              "List-detail layout plus proof context makes the extra width useful for parents and admins without changing the product identity.",
            preview: "board",
            cta: "Inspect chore board",
            footer: "List-detail adaptation"
          }),
          D({
            micro: "Expanded chore board dark",
            step: "10",
            title: "Expanded chore board",
            copy:
              "The dark expanded chore board keeps the exact same list-detail structure, only with semantic token swaps.",
            preview: "board",
            theme: "dark",
            cta: "Inspect dark chore board",
            footer: "Theme parity"
          }),
          D({
            micro: "Wide reports light",
            step: "11",
            title: "Wide reports page",
            copy:
              "Wide reporting combines comparison cards, charts, and the yearly heatmap with meaningful labels and no empty dashboard filler.",
            preview: "reports",
            cta: "Open wide reports",
            footer: "Adaptive analytics"
          }),
          D({
            micro: "Wide reports dark",
            step: "12",
            title: "Wide reports page",
            copy:
              "The dark analytics board matches the exact light layout so it remains implementable through token and style swaps only.",
            preview: "reports",
            theme: "dark",
            cta: "Open dark reports",
            footer: "Same wide structure"
          }),
          S({
            micro: "Account settings",
            step: "13",
            title: "Account settings",
            copy:
              "Account settings gather profile, household role, personal notifications, and device preferences in one clean utility stack.",
            settings: [
              ["Display name", "Erick Sorto", true],
              ["Role visibility", "Show whether you're admin or member across household screens", true],
              ["Quiet hours", "Limit reminders after 9:30 PM unless urgent", false]
            ],
            cta: "Save account settings",
            footer: "Core account surface"
          }),
          S({
            micro: "Theme settings",
            step: "14",
            title: "Theme settings",
            copy:
              "Theme settings explain automatic, light, and dark behavior while previewing how the choice affects the household surfaces.",
            settings: [
              ["Automatic theme", "Follow system or time of day", true],
              ["Light mode", "Use the brighter household palette full time", false],
              ["Dark mode", "Use lower-glare surfaces for evening routines", false]
            ],
            cta: "Save theme",
            footer: "Clear theme control"
          }),
          S({
            micro: "Accessibility settings",
            step: "15",
            title: "Accessibility settings",
            copy:
              "Accessibility controls focus on readability, tap size, contrast, and explicit labels so the product stays usable for the whole family.",
            settings: [
              ["Large labels and chips", "Increase text size and touch targets for key actions", true],
              ["High contrast mode", "Boost contrast on summary cards and badges", false],
              ["Icon with text labels", "Always pair important icons with explicit text", true]
            ],
            cta: "Save accessibility settings",
            footer: "AA-friendly controls"
          }),
          L({
            micro: "Billing admin",
            step: "16",
            title: "Household billing and premium admin",
            copy:
              "Billing admin is operational and trustworthy, with plan status, owner visibility, and calm explanation of what premium still unlocks.",
            chips: ["Premium annual", "Renews Aug 12", "Owner only"],
            items: [
              ["Current plan", "Family Premium Annual"],
              ["What premium adds", "Advanced reports, approval workflows, balancing logic"],
              ["Actions", "Manage plan, change payment, review premium benefits"]
            ],
            cta: "Manage billing",
            footer: "Admin-only billing surface"
          }),
          T({
            micro: "Offline state",
            step: "17",
            title: "Offline state",
            copy:
              "Offline mode explains what is temporarily unavailable and which recent household data is still safe to view.",
            cta: "Try again",
            footer: "Explicit recovery path"
          }),
          T({
            micro: "Error state",
            step: "18",
            title: "Error state",
            copy:
              "Errors use clear household language, not vague technical jargon, and they always point the user to the next useful recovery action.",
            cta: "Retry action",
            footer: "Specific error handling",
            theme: "dark"
          }),
          T({
            micro: "Loading state",
            step: "19",
            title: "Loading state",
            copy:
              "Loading surfaces reuse skeletons and calm progress language so the app feels alive even before the final data arrives.",
            cta: "Loading",
            footer: "Skeleton and progress cues"
          }),
          H({
            micro: "Empty notifications",
            step: "20",
            title: "No notifications right now",
            copy:
              "The notification empty state still reassures the household by explaining that chores, approvals, and streak updates will appear here when needed.",
            image: `${imageBase}/empty-state-no-chores-v1.png`,
            chips: ["No blank inbox", "What appears here", "Calm reassurance"],
            cta: "Back to dashboard",
            footer: "Useful empty inbox"
          })
        ],
        "Product flow"
      )
    ]
  }
];

const params = new URLSearchParams(window.location.search);
const boardFilter = params.get("board");
const visibleBoards = boardFilter ? boards.filter(board => board.step === boardFilter) : boards;

const totalScreens = boards.reduce(
  (sum, board) => sum + board.groups.reduce((groupSum, group) => groupSum + group.screens.length, 0),
  0
);

const totalScreenNode = document.querySelector("[data-total-screens]");
if (totalScreenNode) totalScreenNode.textContent = String(totalScreens);
const totalPageNode = document.querySelector("[data-page-count]");
if (totalPageNode) totalPageNode.textContent = String(boards.length);

const app = document.getElementById("app");
if (boardFilter) {
  document.body.classList.add("single-board");
  document.querySelector(".masthead").style.display = "none";
  document.querySelector(".hero-banner").style.display = "none";
  document.querySelector(".page-shell").style.paddingTop = "18px";
}
app.innerHTML = visibleBoards.map(renderBoard).join("");

function renderBoard(board) {
  const boardId = `board-${board.step.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`;
  const groupsMarkup = board.groups.map(renderGroup).join("");
  const aside = board.aside
    ? `<aside class="divider-note">
        <p class="eyebrow">Board note</p>
        <h3>${board.aside.title}</h3>
        <p>${board.aside.body}</p>
      </aside>`
    : "";

  return `
    <section class="board" id="${boardId}">
      <div class="board-header">
        <p class="eyebrow">${board.step}</p>
        <h2 class="board-title">${board.title}</h2>
        <p class="board-body">${board.body}</p>
        <div class="board-tags">${board.tags.map(tag => `<span class="tag">${tag}</span>`).join("")}</div>
      </div>
      <div class="board-groups">${groupsMarkup}</div>
      ${aside}
    </section>
  `;
}

function renderGroup(group) {
  return `
    <section class="screen-group">
      <div class="group-header">
        <p class="eyebrow">${group.eyebrow}</p>
        <h3>${group.title}</h3>
        <p class="group-copy">${group.body}</p>
      </div>
      <div class="screen-grid">${group.screens.map(renderScreen).join("")}</div>
    </section>
  `;
}

function renderScreen(screen) {
  const themeClass = screen.theme === "dark" ? "screen--dark" : "";
  const desktopClass = ["desktop", "notification", "settings", "states"].includes(screen.kind)
    ? "screen--desktop"
    : "";

  return `
    <article class="screen ${themeClass} ${desktopClass}">
      <div class="screen-top">
        <span class="screen-micro">${screen.micro}</span>
        <span class="step-pill">${screen.step}</span>
      </div>
      <h3 class="screen-title">${screen.title}</h3>
      <p class="screen-copy">${screen.copy}</p>
      ${renderBody(screen)}
      <div class="screen-footer">
        <span class="cta">${screen.cta}</span>
        <span class="cta-note">${screen.footer}</span>
      </div>
    </article>
  `;
}

function renderBody(screen) {
  switch (screen.kind) {
    case "hero":
      return `
        <img class="hero-image" src="${screen.image}" alt="${screen.title}" />
        ${renderChips(screen.chips)}
      `;
    case "list":
      return `
        ${renderChips(screen.chips)}
        <div class="utility-stack">
          ${screen.items
            .map(
              ([label, text]) => `
                <div class="utility-row">
                  <strong>${label}</strong>
                  <span>${text}</span>
                </div>
              `
            )
            .join("")}
        </div>
      `;
    case "chart":
      return `
        ${renderChips(screen.chips)}
        <div class="desktop-preview">
          <div class="chart-row">
            ${screen.bars
              .map(
                (value, index) =>
                  `<span class="bar ${screen.altBars && index % 2 ? "alt" : ""}" style="height:${value * 1.6}px"></span>`
              )
              .join("")}
          </div>
        </div>
      `;
    case "chart-calendar":
      return `
        ${renderChips(screen.chips)}
        <div class="desktop-preview">
          <div class="chart-row">
            ${screen.bars
              .map((value, index) => `<span class="bar ${index % 2 ? "alt" : ""}" style="height:${value * 1.6}px"></span>`)
              .join("")}
          </div>
          <div class="calendar">
            ${[
              "t1", "t2", "t3", "t4", "", "t2", "t4", "t1", "", "t3",
              "", "t1", "t2", "t3", "t4", "t2", "", "t1", "t4", "t3"
            ]
              .map(cls => `<span class="${cls}"></span>`)
              .join("")}
          </div>
        </div>
      `;
    case "desktop":
      return `
        <div class="desktop-preview">
          ${renderDesktopPreview(screen.preview)}
        </div>
      `;
    case "notification":
      return `
        <div class="notification-stack">
          ${screen.notifications
            .map(
              ([title, text], index) => `
                <div class="notification-card">
                  <strong>${title}</strong>
                  <span>${text}</span>
                  ${renderNotificationChips(index)}
                </div>
              `
            )
            .join("")}
        </div>
      `;
    case "settings":
      return `
        <div class="setting-stack">
          ${screen.settings
            .map(
              ([title, text, enabled]) => `
                <div class="setting-row">
                  <strong>${title}</strong>
                  <span>${text}</span>
                  ${renderSettingToggle(enabled)}
                </div>
              `
            )
            .join("")}
        </div>
      `;
    case "states":
      return `
        <div class="state-grid">
          <div class="state-card state-card--error">
            <div class="state-line accent-red"></div>
            <div class="state-line"></div>
            <div class="state-line short"></div>
          </div>
          <div class="state-card state-card--offline">
            <div class="state-line accent-blue"></div>
            <div class="state-line"></div>
            <div class="state-line mid"></div>
          </div>
          <div class="state-card state-card--loading">
            <div class="state-line accent-lavender"></div>
            <div class="state-line"></div>
            <div class="loader-dots"><span></span><span></span><span></span><span></span></div>
          </div>
        </div>
      `;
    default:
      return "";
  }
}

function renderDesktopPreview(type) {
  if (type === "dashboard") {
    return `
      <div class="desktop-shell">
        <div class="pane">
          <div class="screen-chip-row">
            <span class="screen-chip">Today</span>
            <span class="screen-chip screen-chip--mint">4 / 7 done</span>
          </div>
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="pane-line mid"></div>
          <div class="pane-line long"></div>
        </div>
        <div class="pane">
          <div class="pane-line long"></div>
          <div class="pane-line mid"></div>
          <div class="pane-line short"></div>
          <div class="chart-row">
            <span class="bar alt" style="height:54px"></span>
            <span class="bar alt" style="height:88px"></span>
            <span class="bar alt" style="height:66px"></span>
          </div>
        </div>
        <div class="pane pane--tint">
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="pane-line long"></div>
          <div class="pane-line mid"></div>
        </div>
      </div>
    `;
  }

  if (type === "board") {
    return `
      <div class="desktop-shell desktop-shell--wide">
        <div class="pane">
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="pane-line long"></div>
          <div class="pane-line mid"></div>
        </div>
        <div class="pane">
          <div class="pane-line mid"></div>
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="pane-line mid"></div>
        </div>
        <div class="pane pane--indigo">
          <div class="pane-line long"></div>
          <div class="pane-line mid"></div>
          <div class="utility-row">
            <strong>Proof context</strong>
            <span>Photo preview, member note, and decision actions stay visible together.</span>
          </div>
        </div>
      </div>
    `;
  }

  if (type === "reports") {
    return `
      <div class="desktop-shell desktop-shell--wide">
        <div class="pane">
          <div class="chart-row">
            <span class="bar" style="height:36px"></span>
            <span class="bar" style="height:54px"></span>
            <span class="bar" style="height:78px"></span>
            <span class="bar" style="height:106px"></span>
          </div>
        </div>
        <div class="pane pane--tint">
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="pane-line mid"></div>
        </div>
        <div class="pane">
          <div class="calendar">
            ${[
              "t1", "t2", "t3", "t4", "",
              "", "t3", "t4", "t1", "t2",
              "t4", "t1", "t2", "", "t3"
            ]
              .map(cls => `<span class="${cls}"></span>`)
              .join("")}
          </div>
        </div>
      </div>
    `;
  }

  if (type === "foundation") {
    return `
      <div class="desktop-shell desktop-shell--wide">
        <div class="pane">
          <div class="screen-chip-row">
            <span class="screen-chip">Primary action</span>
            <span class="screen-chip screen-chip--amber">Reward tone</span>
          </div>
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
        </div>
        <div class="pane pane--tint">
          <div class="screen-chip-row">
            <span class="screen-chip screen-chip--mint">Progress</span>
            <span class="screen-chip screen-chip--lavender">Support</span>
          </div>
          <div class="pane-line long"></div>
          <div class="pane-line mid"></div>
        </div>
        <div class="pane">
          <div class="pane-line mid"></div>
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
        </div>
      </div>
    `;
  }

  if (type === "shell") {
    return `
      <div class="desktop-shell desktop-shell--wide">
        <div class="pane">
          <div class="pane-line long"></div>
          <div class="screen-chip-row">
            <span class="screen-chip">Home</span>
            <span class="screen-chip screen-chip--mint">Chores</span>
            <span class="screen-chip screen-chip--lavender">Family</span>
          </div>
        </div>
        <div class="pane pane--tint">
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="pane-line mid"></div>
        </div>
        <div class="pane">
          <div class="screen-chip-row">
            <span class="screen-chip screen-chip--amber">Reports</span>
            <span class="screen-chip screen-chip--rose">Settings</span>
          </div>
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
        </div>
      </div>
    `;
  }

  if (type === "family") {
    return `
      <div class="desktop-shell desktop-shell--wide">
        <div class="pane">
          <div class="screen-chip-row">
            <span class="screen-chip">Family</span>
            <span class="screen-chip screen-chip--mint">Members</span>
          </div>
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="pane-line mid"></div>
        </div>
        <div class="pane pane--tint">
          <div class="pane-line long"></div>
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="screen-chip-row">
            <span class="screen-chip screen-chip--amber">Fairness</span>
            <span class="screen-chip screen-chip--lavender">Invite</span>
          </div>
        </div>
        <div class="pane">
          <div class="pane-line mid"></div>
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="pane-line mid"></div>
        </div>
      </div>
    `;
  }

  if (type === "profile") {
    return `
      <div class="desktop-shell desktop-shell--wide">
        <div class="pane">
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="pane-line mid"></div>
        </div>
        <div class="pane pane--tint">
          <div class="pane-line long"></div>
          <div class="chart-row">
            <span class="bar" style="height:42px"></span>
            <span class="bar" style="height:66px"></span>
            <span class="bar" style="height:52px"></span>
          </div>
        </div>
        <div class="pane">
          <div class="screen-chip-row">
            <span class="screen-chip">Streak</span>
            <span class="screen-chip screen-chip--mint">Availability</span>
          </div>
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
        </div>
      </div>
    `;
  }

  if (type === "form") {
    return `
      <div class="desktop-shell desktop-shell--wide">
        <div class="pane pane--tint">
          <div class="pane-line long"></div>
          <div class="pane-line long"></div>
          <div class="pane-line mid"></div>
          <div class="pane-line short"></div>
        </div>
        <div class="pane">
          <div class="pane-line long"></div>
          <div class="screen-chip-row">
            <span class="screen-chip">Repeat</span>
            <span class="screen-chip screen-chip--mint">Proof</span>
            <span class="screen-chip screen-chip--amber">Rewards</span>
          </div>
          <div class="pane-line long"></div>
        </div>
        <div class="pane">
          <div class="pane-line mid"></div>
          <div class="pane-line short"></div>
          <div class="pane-line long"></div>
          <div class="pane-line mid"></div>
        </div>
      </div>
    `;
  }

  if (type === "detail") {
    return `
      <div class="desktop-shell desktop-shell--wide">
        <div class="pane">
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="pane-line mid"></div>
          <div class="pane-line short"></div>
        </div>
        <div class="pane pane--tint">
          <div class="pane-line long"></div>
          <div class="pane-line long"></div>
          <div class="pane-line long"></div>
        </div>
        <div class="pane pane--indigo">
          <div class="pane-line long"></div>
          <div class="pane-line short"></div>
          <div class="utility-row">
            <strong>Decision</strong>
            <span>Approve, ask for redo, or leave an admin note.</span>
          </div>
        </div>
      </div>
    `;
  }

  return "";
}

function renderChips(chips = []) {
  if (!chips.length) return "";
  const toneClasses = ["", "screen-chip--mint", "screen-chip--amber", "screen-chip--lavender", "screen-chip--rose"];
  return `
    <div class="screen-chip-row">
      ${chips
        .map((chip, index) => `<span class="screen-chip ${toneClasses[index % toneClasses.length]}">${chip}</span>`)
        .join("")}
    </div>
  `;
}

function renderNotificationChips(index) {
  const sets = [
    ["Mark done", "Snooze"],
    ["Approved", "Open chore"],
    ["View report", "Later"]
  ];
  return renderChips(sets[index % sets.length]);
}

function renderSettingToggle(enabled) {
  const track = enabled ? "background:#bbf7d0; justify-content:flex-end;" : "background:#dbeafe; justify-content:flex-start;";
  return `
    <div style="margin-top:10px; display:flex; justify-content:flex-end;">
      <span style="${track} width:44px; height:24px; border-radius:999px; display:flex; align-items:center; padding:3px;">
        <span style="width:18px; height:18px; border-radius:50%; background:white; box-shadow:0 4px 10px rgba(16,32,51,0.12);"></span>
      </span>
    </div>
  `;
}
