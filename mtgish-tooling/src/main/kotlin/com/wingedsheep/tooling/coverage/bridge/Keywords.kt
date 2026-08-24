package com.wingedsheep.tooling.coverage.bridge

/** Evergreen keywords that map straight to a `Keyword` enum member. Most other keywords resolve for
 *  free via the probe's PascalCase→enum auto-resolve; these are the ones worth pinning explicitly. */
internal fun BridgeBuilder.keywords() {
    keyword("Flying", "FLYING")
    keyword("Haste", "HASTE")
    keyword("Vigilance", "VIGILANCE")
    keyword("Reach", "REACH")
    keyword("Defender", "DEFENDER")
    // Daybound / Nightbound (CR 702.145, Innistrad: Crimson Vow). Bare card keywords the SDK models as
    // rules-inert `Keyword` enum members — all behaviour (enters-transformed, the designation-change
    // cascade, the can't-transform-except-via-keyword rule) is derived off projected state by the
    // rules engine, so a plain `daybound()`/`nightbound()` stamp is the whole card surface. Both would
    // auto-resolve via PascalCase→enum (Daybound→DAYBOUND), but pin them so the capability reads
    // explicitly and a future enum rename surfaces as a MISSING gap rather than silently dropping.
    // NOTE: these ride on the DFC's front/back faces (daybound front, nightbound back); the emitter
    // stamps them via `keywordLines`, but assembling the two-faced werewolf shape is a per-card read,
    // so pinning the capability doesn't imply an AUTO render of the whole card.
    keyword("Daybound", "DAYBOUND")
    keyword("Nightbound", "NIGHTBOUND")
    // Devoid (CR 702.114, Battle for Zendikar / Oath of the Gatewatch). A characteristic-defining
    // ability — "this object is colorless" — that the IR carries not as a rule name but as the
    // nested `_SettableColor: Devoid` under a `CDA_Color` envelope. A bare `keywords(Keyword.DEVOID)`
    // stamp IS the whole mechanic: the SDK derives `CardDefinition.colors` as empty from the keyword,
    // so it functions in every zone with nothing else to render. It would auto-resolve via
    // PascalCase->enum (Devoid->DEVOID), but pin it like daybound/nightbound so the capability reads
    // explicitly and an enum rename surfaces as a MISSING gap rather than silently dropping.
    keyword("Devoid", "DEVOID")
    // Intimidate (CR 702.13) — `Keyword.INTIMIDATE` exists in the SDK enum, so the PascalCase→enum
    // auto-resolve would accept it, but the rules engine has NO block-evasion handling for it
    // (BlockEvasionRules covers flying/fear/shadow/horsemanship/landwalk only). A bare or granted
    // intimidate would compile, lint, and snapshot fine while doing nothing in combat — a silent
    // no-op. Pin it blocking until the engine implements it; then delete this line.
    unsupported("Intimidate", "Keyword.INTIMIDATE is enum-only — no BlockEvasionRules handling; implement intimidate block evasion (CR 702.13) to unlock")
    // "Hexproof from [quality]" (CR 702.11b) — a PARAMETERIZED keyword ability, so `supported` rather
    // than `keyword`: a bare `keywords(Keyword.HEXPROOF)` stamp would drop the quality and silently
    // widen the card to full hexproof. Modelled as
    // `keywordAbility(KeywordAbility.Hexproof(ProtectionScope.…))`; the engine enforces the Color and
    // CardType scopes (Knight of Malice, Elenda, Saint of Dusk) at all three targeting sites. This entry
    // only marks the capability covered — the emitter declines (SCAFFOLD) because picking the right
    // ProtectionScope from the IR's quality node is a per-card read.
    supported("HexproofFrom", "keyword ability: hexproof from [quality] -> KeywordAbility.Hexproof(ProtectionScope.…) (CR 702.11b)")
    // Gift a [something] (CR 702.174, Bloomburrow) — a PARAMETERIZED keyword ability (the gifted thing
    // rides in the rule), so `supported` rather than `keyword`: there is no Keyword.GIFT enum member.
    // On a permanent the engine has the whole mechanic: `gift(GiftKind.…)` adds the cast-time additional
    // cost (a `CastWithGift` legal action per opponent) plus the derived "when this enters, if its gift
    // cost was paid, …" trigger; on an instant/sorcery it's `Patterns.Mechanic.giftSpell(…)`. The emitter
    // declines (SCAFFOLD): which GiftKind is listed, and how the card's *other* text branches on
    // `Conditions.GiftWasPromised`, is a per-card read.
    supported("Gift", "keyword ability: Gift a [something] -> gift(GiftKind.…) on permanents / Patterns.Mechanic.giftSpell on instants & sorceries (CR 702.174)")
    // Bargain (CR 702.166, Wilds of Eldraine) — "You may sacrifice an artifact, enchantment, or token
    // as you cast this spell." `composed`, not a bare `keyword`: `Keyword.BARGAIN` exists, but stamping
    // it alone would print the word and drop the whole mechanic — the optional additional cost and the
    // ChoiceSlot.BARGAINED declaration its payoffs read. The `bargain()` CardBuilder helper composes
    // both (an OptionalAdditionalCost over ArtifactEnchantmentOrToken declaring that slot), and the
    // emitter's `rname == "Bargain"` branch renders that no-arg builder call (the rule carries no args).
    // The payoff side — `SpellPassesFilter(ThisSpell, WasBargained)` — renders as
    // `Conditions.WasBargained` in actionConditionDsl, so a plain "if this spell was bargained, X else
    // Y" rider emits whole.
    composed(
        "Bargain",
        "keyword: bargain() -> optional 'sacrifice an artifact, enchantment, or token' additional cost + ChoiceSlot.BARGAINED (CR 702.166)",
        composes = listOf("Sacrifice"),
    )
    // Saddle N (CR 702.171) — a PARAMETERIZED keyword ability (the N count rides in the rule's args),
    // NOT a bare card keyword. It must be `supported`, not `keyword`: a `keyword` entry would make
    // `keywordLines` stamp a bare `keywords(Keyword.SADDLE)` on the card and drop the N, exactly the
    // parameterized-keyword trap the `keywordLines` guard warns about (and why Crew carries no bridge
    // entry at all). The emitter's explicit `rname == "Saddle"` branch renders
    // `keywordAbility(KeywordAbility.saddle(N))`; this `supported` entry only marks the capability as
    // covered (never blocking) so the probe doesn't report Saddle as a gap.
    supported("Saddle", "keyword ability: Saddle N -> keywordAbility(KeywordAbility.saddle(N)) (CR 702.171)")
    // "Crew N. Activate only once each turn." (Luxurious Locomotive) — a PARAMETERIZED keyword ability
    // like Saddle/Crew, so `supported` (not `keyword`) to avoid dropping the N. The emitter's
    // `rname == "CrewOnceEachTurn"` branch renders `keywordAbility(KeywordAbility.crew(N, onceEachTurn = true))`;
    // the engine enforces the once-per-turn cap in the crew enumerator/handler.
    supported("CrewOnceEachTurn", "keyword ability: Crew N, activate only once each turn -> keywordAbility(KeywordAbility.crew(N, onceEachTurn = true))")
    // Firebending N (CR 702.189, Avatar: The Last Airbender) — a PARAMETERIZED keyword ability (the
    // N count rides in the rule's args as a nested _GameNumber:Integer, exactly like Saddle). Must be
    // `supported`, not `keyword`: a bare `keywords(Keyword.FIREBENDING)` would drop the N. The
    // emitter's `rname == "Firebending"` branch renders `keywordAbility(KeywordAbility.firebending(N))`;
    // the integer case auto-renders, while "firebending X (X = its power)" carries an XValue node and
    // the emitter declines it (-> SCAFFOLD). This entry only marks the capability covered.
    supported("Firebending", "keyword ability: Firebending N -> keywordAbility(KeywordAbility.firebending(N)) (CR 702.189)")
    // Impending N—[cost] (CR 702.176, Duskmourn) — a PARAMETERIZED self-alternative-cost keyword whose
    // whole mechanic (alt cost + N time counters + "not a creature while it has a time counter" static +
    // end-step countdown trigger) is composed by the `impending(n, cost)` CardBuilder helper. Must be
    // `supported`, not a bare `keyword`: a plain `keywords(Keyword.IMPENDING)` would drop both the count
    // and the cost and lose the counter machinery. The emitter's `rname == "Impending"` branch renders
    // the `impending(N, "{cost}")` builder call (pure-mana cost + fixed Int count only; otherwise it
    // declines -> SCAFFOLD). This entry only marks the capability covered (never blocking).
    supported("Impending", "keyword ability: Impending N—[cost] -> impending(N, \"{cost}\") builder (CR 702.176)")
    // Increment (Secrets of Strixhaven) — a keyword whose whole mechanic is composed by the
    // `increment()` CardBuilder helper: the display keyword plus a "whenever you cast a spell, if the
    // mana you spent exceeds this creature's power or toughness, put a +1/+1 counter on it" triggered
    // ability (intervening-if on EntityNumericProperty.ManaSpent). `composed`, not a bare keyword: a
    // plain Keyword.INCREMENT stamp would drop the cast-spell trigger. The emitter's `rname ==
    // "Increment"` branch renders the no-arg `increment()` builder call (the rule carries no args).
    composed(
        "Increment",
        "keyword: increment() -> Keyword.INCREMENT + 'whenever you cast a spell, if mana spent > power or toughness, +1/+1 counter' trigger",
        composes = listOf("AddCounters"),
    )
    // Paradigm (Secrets of Strixhaven) — a spell ability word lowered to the `paradigm()` CardBuilder
    // helper: it implies self-exile on resolution plus a synthesized "at the beginning of each of your
    // first main phases, you may cast a free copy from exile" recast trigger (Paradigm.recastAbility).
    // `composed`, not a bare effect: it has no standalone Effect, and the recast recurrence is engine-
    // synthesized from the exile marker. The emitter strips the trailing `Paradigm(ThisSpell)` action
    // off the spell's ActionList and emits `paradigm()` in the spell block (see CardStructure.spellBlock).
    composed(
        "Paradigm",
        "spell ability word: paradigm() -> self-exile on resolve + synthesized first-main-phase free-recast trigger",
        composes = listOf("ExileSpell"),
    )
    // Ward (CR 702.21) — a PARAMETERIZED keyword ability: the cost rides in the rule's args
    // (`Ward—Discard a card`, `Ward {2}`, `Ward—Pay N life`, `Ward—Sacrifice <filter>`). Like Saddle,
    // it must be `supported`, not `keyword`: a bare `keywords(Keyword.WARD)` would drop the cost. The
    // emitter's `rname == "Ward"` branch renders `keywordAbility(KeywordAbility.ward(...)/wardDiscard()/
    // wardLife(N)/wardLife(DynamicAmounts.sourcePower())/wardSacrifice(filter))` for the cost shapes it
    // can express ("Ward—Pay life equal to ~'s power", Raubahn, renders the dynamic form); richer/compound
    // costs decline -> SCAFFOLD. This entry only marks the capability covered (never blocking).
    supported("Ward", "keyword ability: Ward—<cost> (CR 702.21) -> keywordAbility(KeywordAbility.ward(...)/wardDiscard()/wardLife(N)/wardSacrifice(filter))")
    // Madness [cost] (CR 702.35) — a PARAMETERIZED keyword whose whole mechanic (the discard →
    // exile replacement plus the "may cast it for [cost]" trigger the engine synthesizes on that
    // exile) hangs off the cost. `supported`, not `keyword`: `Keyword.MADNESS` exists, so the
    // PascalCase→enum auto-resolve would stamp a bare `keywords(Keyword.MADNESS)` and drop the
    // cost — the same trap as Ward/Saddle. The emitter's `rname == "Madness"` branch renders the
    // `madness("{cost}")` builder call for the pure-mana shape (all of them, in practice); anything
    // else declines -> SCAFFOLD. This entry only marks the capability covered (never blocking).
    supported("Madness", "keyword ability: Madness [cost] -> madness(\"{cost}\") builder (CR 702.35)")
    // Disturb [cost] (CR 702.146) — a PARAMETERIZED keyword ability, so `supported` rather than
    // `keyword`: `Keyword.DISTURB` exists, so PascalCase→enum auto-resolve would stamp a bare
    // `keywords(Keyword.DISTURB)` and drop both the cost and the transform — the Ward/Saddle/Madness
    // trap. Modelled as `disturb("{cost}")` on the FRONT face; the engine owns the whole mechanic
    // (graveyard cast enumeration, the back-face flip before the spell is put on the stack, back-face
    // timing/targets per CR 712.8c, front-face mana value).
    // NOTE: this pins the *capability* only. Every disturb card is a transforming DFC, and the emitter
    // declines every multi-faced card (the bridge sees only the front face's IR), so these stay
    // SCAFFOLD and get hand-authored — exactly like Daybound/Nightbound above.
    supported("Disturb", "keyword ability: Disturb [cost] -> disturb(\"{cost}\") builder on the DFC front face (CR 702.146)")
    // Emerge [cost] (CR 702.119) — a PARAMETERIZED keyword ability, so `supported` rather than
    // `keyword`: `Keyword.EMERGE` exists, so PascalCase→enum auto-resolve would stamp a bare
    // `keywords(Keyword.EMERGE)` and drop the cost — the Ward/Saddle/Madness trap. Modelled as
    // `emerge("{cost}")`; the engine owns the whole mechanic (the hand alternative cost, the
    // creature sacrifice, and the generic-only reduction by the sacrificed creature's mana value).
    // The emitter's `rname == "Emerge"` branch renders the builder call for the pure-mana shape
    // (every printed emerge); anything else declines -> SCAFFOLD.
    supported("Emerge", "keyword ability: Emerge [cost] -> emerge(\"{cost}\") builder (CR 702.119)")
    // Splice onto [quality] [cost] (CR 702.47) — a PARAMETERIZED keyword ability, so `supported` rather
    // than `keyword`: `Keyword.SPLICE` exists, so PascalCase→enum auto-resolve would stamp a bare
    // `keywords(Keyword.SPLICE)` and drop the cost — the Ward/Saddle/Madness/Emerge trap. Modelled as
    // `splice("{cost}")`; the engine owns the whole mechanic (the `CastWithSplice` cast variant, the
    // additional cost, the reveal that leaves the card in hand, and appending the card's own spell
    // effect + targets to the spell on the stack after its own effects).
    // The card needs no other authoring: whatever `spell { }` script it would use when cast normally
    // *is* the text that gets spliced, so a splice card renders exactly like the plain spell plus this
    // one line. The emitter's `rname == "Splice"` branch renders the builder call for the pure-mana
    // shape (every printed splice cost); anything else declines -> SCAFFOLD.
    supported("Splice", "keyword ability: Splice onto Arcane [cost] -> splice(\"{cost}\") builder (CR 702.47)")

    // Disguise (CR 702.168) — morph plus ward {2}. Like Morph it's a keyword ability with no
    // `Keyword.DISGUISE` enum member (the {3} face-down cast and the turn-face-up special action are
    // synthesised from the ability, and the ward {2} is a face-down characteristic carried by
    // `FaceDownMode.DISGUISE`), so it's `supported` rather than `keyword` — a bare-keyword row would
    // stamp a non-existent enum. The emitter renders the pure-mana shape as the card-level
    // `disguise = "{cost}"` assignment (Emitter.kt `rname == "Disguise"`).
    supported("Disguise", "keyword ability: Disguise [cost] (CR 702.168) -> disguise = \"{cost}\"; face-down 2/2 with ward {2}")
    // "Disguise {X}{W}{W}" (Aurelia's Vindicator) — CR 702.168e makes the X chosen as the turn-face-up
    // special action was taken readable by the permanent's other abilities. `KeywordAbility.Disguise`
    // takes a full PayCost so the cost itself is expressible, but inheriting that X into a later
    // ability is the cast-time-chosen-value area the module README flags as still sloppy, so the
    // emitter declines -> SCAFFOLD.
    supported("DisguiseX", "keyword ability: Disguise {X}... (CR 702.168e) -> disguiseCost with X; emitter scaffolds (X inherited by other abilities)")

    composed("Landwalk", "specific *WALK keywords (SWAMPWALK, FORESTWALK, ...)")
    // Equip is a keyword ability, but the engine has no `Keyword.EQUIP` enum member: `equipAbility(cost)`
    // synthesises the sorcery-speed "attach to target creature you control" activated ability whose
    // resolution is `AttachEquipment`. So it's composed, not a bare keyword (which would emit a
    // non-existent enum). The emitter renders only the canonical unrestricted shape (see Emitter.equipAbilityLine).
    composed("Equip", "equip: equipAbility(cost) -> sorcery-speed AttachEquipment activated ability", composes = listOf("AttachEquipment"))
    // Cycling (CR 702.29) is a keyword ability with no `Keyword.CYCLING` enum member — `KeywordAbility.cycling(cost)`
    // synthesises the activated "Discard this card: Draw a card" ability. Like Equip it's composed, not a bare
    // keyword (PascalCase auto-resolve would look for a non-existent CYCLING enum and block it). The emitter renders
    // the canonical pure-mana shape (see Emitter.kt `rname == "Cycling"`).
    composed("Cycling", "cycling: KeywordAbility.cycling(cost) -> 'Discard this card: Draw a card' activated ability", composes = listOf("DrawCards"))
    // Typecycling (CR 702.29) — the subtype/land-type cycling variants ("Forestcycling", "Slivercycling", …).
    // `KeywordAbility.typecycling(subtype, cost)` synthesises a "Discard this card: search your library for a
    // <subtype> card" activated ability. Composed like Cycling/Equip (no CYCLING enum); the emitter renders the
    // pure-mana land-type/creature-type shape (see Emitter.kt `rname == "TypeCycling"` + EmitCtx.typecyclingLine).
    composed("TypeCycling", "typecycling: KeywordAbility.typecycling(subtype, cost) -> 'Discard this card: search for a <subtype> card' activated ability", composes = listOf("MoveCollection", "ShuffleLibrary"))

    // Station (CR 702.184, Edge of Eternities). Three IR rules. Like Saddle, these are activated/static
    // abilities the engine fully supports but that aren't bare card keywords, so they're `supported`
    // (never blocking) rather than `keyword` (which would stamp a non-existent Keyword.STATION enum).
    //  - `Station` itself: the keyword ability -> the `station()` builder (Emitter `rname == "Station"`).
    //  - `StationChargedAnimate`: the {N+} symbol that turns the permanent into a creature -> threshold-gated
    //    GrantCardType + GrantKeyword rows (Emitter `rname == "StationChargedAnimate"`).
    //  - `StationCharged`: the non-animating {N+} symbol that gates an activated/triggered ability. The engine
    //    expresses it (ActivationRestriction.OnlyIfCondition + Conditions.SourceCounterCountAtLeast), so it's
    //    covered; the emitter declines to auto-render its arbitrary payload and scaffolds (default branch).
    supported("Station", "keyword ability: Station (CR 702.184a) -> station() builder; adds charge counters = tapped creature's power")
    supported("StationChargedAnimate", "{N+} station symbol -> animate into a creature: gated GrantCardType + GrantKeyword rows (CR 721.2b)")
    supported("StationCharged", "{N+} station symbol gating an ability (CR 721.2a) -> OnlyIfCondition(SourceCounterCountAtLeast(CHARGE, N)); emitter scaffolds the payload")
}
