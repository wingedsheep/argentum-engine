package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable
import com.wingedsheep.sdk.dsl.decayed
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.dsl.impending
import com.wingedsheep.sdk.dsl.mobilize
import com.wingedsheep.sdk.dsl.renew
import com.wingedsheep.sdk.dsl.sneak

@Serializable
enum class Keyword(val displayName: String) {
    // ── Evasion ──────────────────────────────────────────────
    FLYING("Flying"),
    MENACE("Menace"),
    INTIMIDATE("Intimidate"),
    FEAR("Fear"),
    SHADOW("Shadow"),
    HORSEMANSHIP("Horsemanship"),

    // ── Landwalk ─────────────────────────────────────────────
    SWAMPWALK("Swampwalk"),
    FORESTWALK("Forestwalk"),
    ISLANDWALK("Islandwalk"),
    MOUNTAINWALK("Mountainwalk"),
    PLAINSWALK("Plainswalk"),
    DESERTWALK("Desertwalk"),
    NONBASIC_LANDWALK("Nonbasic landwalk"),

    // ── Combat ───────────────────────────────────────────────
    FIRST_STRIKE("First strike"),
    DOUBLE_STRIKE("Double strike"),
    TRAMPLE("Trample"),
    DEATHTOUCH("Deathtouch"),
    LIFELINK("Lifelink"),
    VIGILANCE("Vigilance"),
    REACH("Reach"),
    PROVOKE("Provoke"),
    FLANKING("Flanking"),

    /**
     * Banding (CR 702.22). As they declare attackers, a player may group one or more
     * attacking creatures with banding plus up to one without banding into a "band"
     * (CR 702.22c). A band attacks the same defender and is blocked as a group.
     *
     * Banding inverts who assigns combat damage (an exception to CR 510.1c):
     * - CR 702.22j — if an attacker is blocked by a creature with banding, the
     *   *defending* player divides that attacker's combat damage among its blockers.
     * - CR 702.22k — if a blocker is blocking a creature with banding, the *active*
     *   player divides that blocker's combat damage among the attackers it blocks.
     */
    BANDING("Banding"),

    // ── Defense ──────────────────────────────────────────────
    DEFENDER("Defender"),
    INDESTRUCTIBLE("Indestructible"),
    HEXPROOF("Hexproof"),
    SHROUD("Shroud"),
    WARD("Ward"),
    PROTECTION("Protection"),
    PROTECTION_FROM_EACH_OPPONENT("Protection from each opponent"),

    // ── Speed ────────────────────────────────────────────────
    HASTE("Haste"),
    FLASH("Flash"),

    // ── Triggered/Static keyword abilities ───────────────────
    PROWESS("Prowess"),

    /**
     * Flurry (Tarkir: Dragonstorm, Jeskai). "Flurry — Whenever you cast your second spell
     * each turn, [effect]." A display-only keyword tag; the behavior lives in a triggered
     * ability on the [com.wingedsheep.sdk.scripting.EventPattern.NthSpellCastEvent] (n=2, you)
     * event, wired by the `flurry { }` DSL helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    FLURRY("Flurry"),
    CHANGELING("Changeling"),

    /**
     * Devoid (CR 702.114). "Devoid" means "This object is colorless." — a
     * *characteristic-defining* ability (CR 604.3), not a continuous effect, so it functions in
     * every zone and even outside the game, and it applies before any other layer-5 effect
     * (CR 613.3).
     *
     * Because of that, the SDK models it where an object's colors are *derived* rather than as a
     * static ability: [com.wingedsheep.sdk.model.CardDefinition.colors] reads empty for a card
     * carrying this keyword, so every zone-agnostic reader (the engine's `CardComponent.colors`,
     * projection's layer-5 base row, protection and evasion checks, the client's card view, search)
     * sees a colorless object with no wiring of its own. A later "becomes blue" effect still wins,
     * exactly as CR 613.3 orders it.
     *
     * Devoid does **not** touch color *identity* (CR 903.4): an Eldrazi with devoid and a {2}{U}{U}
     * mana cost is colorless but still blue-identity for Commander, which is why
     * [com.wingedsheep.sdk.model.CardDefinition.colorIdentity] reads the mana cost directly.
     *
     * Multiple instances are redundant, and nothing prints a *granted* devoid — it is a printed CDA
     * only.
     */
    DEVOID("Devoid"),

    // ── ETB modification ──────────────────────────────────────
    AMPLIFY("Amplify"),

    /**
     * Riot (CR 702.136). "This permanent enters the battlefield with your choice of a +1/+1 counter
     * or haste." A display tag; the mechanic is composed in the DSL via
     * [com.wingedsheep.sdk.dsl.riot], which pairs an
     * [com.wingedsheep.sdk.scripting.EntersWithChoice] (ChoiceType.MODE, counter|haste) with a
     * mode-gated [com.wingedsheep.sdk.scripting.EntersWithCounters] (counter branch) and a mode-gated
     * haste grant. Unlike most composed keywords, RIOT is **grant-aware**: when a lord effect grants
     * riot to other permanents ("Other Spiders you control have riot" — Spider-Punk), the engine
     * synthesizes the same enters-with choice for any permanent that enters carrying the projected
     * RIOT keyword.
     */
    RIOT("Riot"),

    /**
     * Devour (CR 702.82). "Devour N" — "As this creature enters, you may sacrifice
     * any number of creatures. This creature enters with N times that many +1/+1
     * counters on it." Variants substitute the sacrificed permanent type: e.g.
     * "Devour land 3" sacrifices lands instead of creatures (Edge of Eternities).
     * The sacrifice filter and multiplier live on
     * [com.wingedsheep.sdk.scripting.KeywordAbility.Devour].
     */
    DEVOUR("Devour"),

    /**
     * Craft (CR 702.167, The Lost Caverns of Ixalan). On a transforming
     * double-faced permanent. "Craft with [filter] [cost] ([cost], Exile this
     * permanent, Exile [filter] you control and/or [filter] cards from your
     * graveyard: Return this card transformed under its owner's control.
     * Craft only as a sorcery.)"
     *
     * Display tag — the full mechanic is composed in the DSL via
     * [com.wingedsheep.sdk.dsl.CardBuilder.craft], which pairs
     * [com.wingedsheep.sdk.scripting.AbilityCost.Craft] with
     * [com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect].
     * The back face's "exiled cards used to craft it" CDA (CR 702.167c) reads
     * [com.wingedsheep.sdk.scripting.values.DynamicAmount.CraftedMaterialsTotalPower].
     */
    CRAFT("Craft"),

    // ── Cost reduction ───────────────────────────────────────
    CONVOKE("Convoke"),
    DELVE("Delve"),
    AFFINITY("Affinity"),

    /**
     * Improvise (CR 702.126). "For each generic mana in this spell's total cost, you may tap an
     * untapped artifact you control rather than pay that mana."
     *
     * A static ability that functions only while the spell is on the stack (CR 702.126a). It is
     * neither an additional nor an alternative cost and applies only *after* the total cost is
     * determined (CR 702.126b), so it can never pay a colored pip and never changes the spell's
     * mana value; multiple instances are redundant (CR 702.126c).
     *
     * Mechanically it is the artifacts-only case of the shared "tap permanents, each paying {1}
     * generic" rail — the taps travel in
     * [com.wingedsheep.sdk.scripting.AlternativePaymentChoice.tapForGenericPermanents], exactly
     * like a waterbend cost's taps. Grantable to other spells via
     * [com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells] (Ironheart, Clever Champion).
     */
    IMPROVISE("Improvise"),

    /**
     * Emerge [cost] (CR 702.119, Eldritch Moon). "You may cast this spell by paying [cost] and
     * sacrificing a creature rather than paying its mana cost. If you chose to pay this spell's
     * emerge cost, its total cost is reduced by an amount of generic mana equal to the sacrificed
     * creature's mana value."
     *
     * An alternative cost that bundles a sacrifice *and* a cost reduction derived from what was
     * sacrificed. See [com.wingedsheep.sdk.scripting.KeywordAbility.Emerge].
     */
    EMERGE("Emerge"),

    // ── Spell mechanics ─────────────────────────────────────
    STORM("Storm"),
    FLASHBACK("Flashback"),

    /**
     * Harmonize—[cost] (Tarkir: Dragonstorm). "You may cast this card from your
     * graveyard for its harmonize cost. You may tap a creature you control to
     * reduce that cost by an amount of generic mana equal to its power. Then exile
     * this spell."
     *
     * Modelled like [FLASHBACK] (graveyard cast + exile-on-resolution) plus a
     * Convoke-style single-creature reduction routed through the alternative-payment
     * pipeline. See [com.wingedsheep.sdk.scripting.KeywordAbility.Harmonize].
     */
    HARMONIZE("Harmonize"),

    /**
     * Mayhem [cost] (CR 702.187, Marvel's Spider-Man). "As long as you discarded this card
     * this turn, you may cast it from your graveyard by paying [cost] rather than paying its
     * mana cost." Unlike [FLASHBACK]/[HARMONIZE] the spell is NOT exiled on resolution — a
     * permanent simply enters the battlefield and an instant/sorcery goes to the graveyard as
     * normal. Grants no timing permission (normal timing rules still apply). Gated on the
     * turn-scoped "you discarded this card this turn" tracker
     * (`Conditions.YouDiscardedThisCardThisTurn`). See
     * [com.wingedsheep.sdk.scripting.KeywordAbility.Mayhem].
     */
    MAYHEM("Mayhem"),

    /**
     * Madness [cost] (CR 702.35). "If you discard this card, discard it into exile. When you do,
     * cast it for its madness cost or put it into your graveyard."
     *
     * Two abilities in one keyword (CR 702.35a): a **static** ability that functions in hand and
     * replaces the discard's destination with exile, and a **triggered** ability that fires on that
     * exile and offers its owner a one-shot cast for [cost]. Because the cast happens while the
     * trigger resolves, timing restrictions don't apply — a madness sorcery can be cast during an
     * opponent's turn. Declining (or being unable to pay) puts the card into the graveyard.
     *
     * Unlike [MAYHEM] — the other "you discarded it" recast — madness offers the cast *once*, at
     * the moment of the discard, and the card never touches the graveyard on the way. The engine
     * keys the whole mechanic off [com.wingedsheep.sdk.scripting.KeywordAbility.Madness]; the
     * triggered half is the synthesized [com.wingedsheep.sdk.scripting.Madness.castAbility].
     */
    MADNESS("Madness"),

    /**
     * Disturb [cost] (CR 702.146). "You may cast this card transformed from your graveyard by
     * paying [cost] rather than its mana cost."
     *
     * Printed on the *front* face of a transforming double-faced card; the resulting spell has its
     * **back** face up and only the back face's characteristics (CR 712.8c) — so its type line,
     * targets (an Aura back face chooses what to enchant), P/T, and abilities all come from the back
     * face, while its mana value still comes from the front face's mana cost. Unlike
     * [FLASHBACK]/[HARMONIZE] the card is not exiled on resolution; the back faces carry their own
     * "if this would be put into a graveyard from anywhere, exile it instead" replacement.
     * See [com.wingedsheep.sdk.scripting.KeywordAbility.Disturb].
     */
    DISTURB("Disturb"),
    EVOKE("Evoke"),

    /**
     * Sneak [cost] (CR 702.190, Teenage Mutant Ninja Turtles).
     * "Any time you could cast an instant during your declare blockers step, you may cast
     * this spell by paying [cost] and returning an unblocked creature you control to its
     * owner's hand rather than paying this spell's mana cost." A permanent spell whose
     * sneak cost was paid enters tapped and attacking (CR 702.190b).
     *
     * Modelled as an alternative cost ([KeywordAbility.Sneak]) with a declare-blockers
     * timing permission and a return-an-unblocked-attacker additional cost. Wired by the
     * `sneak(cost)` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    SNEAK("Sneak"),

    /**
     * Ninjutsu [cost] (CR 702.49).
     * "[cost], Return an unblocked attacker you control to hand: Put this card onto the
     * battlefield from your hand tapped and attacking." Activated only during the declare
     * blockers step (or any later combat step) once blocked/unblocked status is assigned.
     *
     * Mechanically identical to [SNEAK] — both are modelled by the same declare-blockers
     * alternative-cost pipeline (return an unblocked attacker, enter tapped and attacking the
     * same defender, CR 506.3a). Ninjutsu is the canonical rules keyword; Sneak is its reflavor
     * in the custom Teenage Mutant Ninja Turtles set. Wired by the `ninjutsu(cost)` DSL helper
     * on [com.wingedsheep.sdk.dsl.CardBuilder]; the shared behavior keys off
     * [KeywordAbility.ninjutsuStyleCost].
     */
    NINJUTSU("Ninjutsu"),

    /**
     * Web-slinging [cost] (CR 702.188, Marvel's Spider-Man).
     * "You may cast this spell by paying [cost] and returning a tapped creature you control to
     * its owner's hand rather than paying its mana cost." (CR 702.188a)
     *
     * Modelled as a hand-timed alternative cost ([KeywordAbility.WebSlinging]) bundling a
     * return-a-tapped-creature additional cost — casting follows the alternative-cost rules
     * (CR 601.2b / 601.2f–h). Unlike [SNEAK]/[NINJUTSU] it grants no timing permission: the spell
     * is web-slung at its normal timing (sorcery speed for creatures, instant speed for
     * Spider-Sense). The "cast using web-slinging" fact rides the resulting permanent durably
     * ([com.wingedsheep.sdk.scripting.ChoiceSlot.WEB_SLUNG], read via
     * [com.wingedsheep.sdk.dsl.Conditions.WebSlungCostWasPaid]) alongside the returned creature's
     * mana value ([com.wingedsheep.sdk.scripting.ChoiceSlot.WEB_SLUNG_RETURNED_MV], read via
     * [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastChoice]). Wired by the
     * `webSlinging(cost)` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    WEB_SLINGING("Web-slinging"),

    /**
     * Impending N—[cost] (CR 702.175, Duskmourn: House of Horror).
     * "If you cast this spell for its impending cost, it enters with N time counters
     * and isn't a creature until the last is removed. At the beginning of your end step,
     * remove a time counter from it."
     *
     * Modelled as a self-alternative cost ([KeywordAbility.Impending]). The
     * `impending(n, cost)` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder] wires the
     * full behavior: the alternative cost, the conditional "isn't a creature while it
     * has a time counter" type-removing static ability, and the "remove a time counter
     * at the beginning of your end step" triggered ability. The engine adds the N TIME
     * counters when a spell cast for its impending cost resolves.
     */
    /**
     * Splice onto [quality] [cost] (CR 702.47, Champions of Kamigawa).
     * "You may reveal this card from your hand as you cast a [quality] spell. If you do, that spell
     * gains the text of this card's rules text and you pay [cost] as an additional cost to cast that
     * spell." (CR 702.47a)
     *
     * Modelled as [KeywordAbility.Splice] — a static ability that functions while the card is in
     * hand, so the card itself is never cast and never leaves hand (CR 702.47a): it is only
     * *revealed*, and the spell it is spliced onto gains its rules text. The spliced text is added
     * as an ordered tail after the main spell's own effects (CR 702.47b) and the spell keeps every
     * one of its own characteristics — colour, name, types (CR 702.47c) — so the splice is invisible
     * to protection and to "target Arcane spell" style checks. Splice changes are lost as soon as
     * the spell leaves the stack (CR 702.47e), which falls out of the choice riding the stack
     * object.
     *
     * Wired by the `splice(cost)` / `splice(onto, cost)` DSL helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    SPLICE("Splice"),
    IMPENDING("Impending"),
    CONSPIRE("Conspire"),

    /**
     * Casualty N (CR 702.153). "As an additional cost to cast this spell, you may sacrifice a
     * creature with power N or greater. When you do, copy this spell and you may choose new
     * targets for the copy." Modeled like Conspire: an optional additional cost (sacrifice one
     * creature meeting the power threshold) plus a reflexive triggered copy. The threshold N is
     * carried by [com.wingedsheep.sdk.scripting.KeywordAbility.Casualty] (printed) or by
     * [com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells.keywordParameter] (granted).
     */
    CASUALTY("Casualty"),

    /**
     * Bargain (CR 702.166, Wilds of Eldraine). A static ability that functions while the spell is
     * on the stack: "As an additional cost to cast this spell, you may sacrifice an artifact,
     * enchantment, or token." A spell whose controller declared that intention has been
     * *bargained* (CR 702.166b), and the card's other abilities — linked to this one
     * (CR 702.166c) — branch on that fact.
     *
     * Modelled as an optional additional cost on the shared cast-time rail:
     * [com.wingedsheep.sdk.scripting.KeywordAbility.OptionalAdditionalCost] with
     * `declaredSlot = `[com.wingedsheep.sdk.scripting.ChoiceSlot.BARGAINED], so "bargained" is a
     * *different* fact from "kicked" — a bargained spell never triggers a "whenever you cast a
     * kicked spell" payoff, and vice versa. Wired by the `bargain()` DSL helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder]; payoffs read it back through
     * [com.wingedsheep.sdk.dsl.Conditions.WasBargained].
     */
    BARGAIN("Bargain"),

    /**
     * Miracle {cost} (CR 702.94). "You may cast this card for its miracle cost when you draw it if
     * it's the first card you drew this turn." Modeled as a hand-only alternative cost gated by a
     * one-turn window: when a card with miracle (printed via
     * [com.wingedsheep.sdk.scripting.KeywordAbility.Miracle] or granted via
     * [com.wingedsheep.sdk.scripting.GrantMiracleToCardsInHand]) is the first card a player draws in
     * a turn, the engine stamps it with a miracle window for that turn; the cast-from-hand
     * enumerator then surfaces a "Cast (Miracle)" alternative cost at the miracle mana cost.
     */
    MIRACLE("Miracle"),

    /**
     * Soulbond (CR 702.95, Avacyn Restored). Two triggered abilities that pair this creature with
     * another unpaired creature you control — "When this creature enters, … you may pair this
     * creature with another unpaired creature you control" and "Whenever another creature you
     * control enters, … you may pair that creature with this creature". Both are authored by the
     * [com.wingedsheep.sdk.dsl.soulbond] builder, which adds this keyword for display plus the
     * two abilities; the payoff is a separate static ability scoped to
     * [com.wingedsheep.sdk.scripting.filters.unified.Scope.SoulbondPair] ("as long as this
     * creature is paired with another creature, both creatures …").
     *
     * The pairing itself is engine state, not a keyword behaviour: `PairWithSourceEffect` stamps
     * a `PairedComponent` on each half, and the CR 702.95e state check breaks the pair when either
     * half leaves the battlefield, stops being a creature, or changes controller.
     */
    SOULBOND("Soulbond"),
    HIDEAWAY("Hideaway"),

    /**
     * Cascade (CR 702.85). "When you cast this spell, exile cards from the top of
     * your library until you exile a nonland card whose mana value is less than
     * this spell's mana value. You may cast that spell without paying its mana
     * cost. Put the exiled cards on the bottom of your library in a random order."
     * The cascade trigger fires at cast time and is implemented by the engine when
     * a spell carries the CASCADE keyword (or is granted it by another effect).
     */
    CASCADE("Cascade"),

    /**
     * Plot (CR 718, Outlaws of Thunder Junction). "Plot [cost]" — special action
     * available any time you have priority during your main phase while the stack is
     * empty: pay the plot cost and exile this card from your hand. It becomes plotted.
     * On any later turn, you may cast a plotted card from exile without paying its
     * mana cost as a sorcery.
     *
     * The keyword itself is display-only; cast/exile wiring lives in
     * [com.wingedsheep.sdk.scripting.KeywordAbility.Plot] and the engine's plot
     * action handler + enumerator.
     */
    PLOT("Plot"),

    /**
     * Foretell (CR 702.143, Kaldheim). "Foretell [cost]" — a keyword ability that functions while
     * the card is in a player's hand. Special action (CR 116.2h): any time you have priority during
     * your turn you may pay {2} and exile the card from your hand *face down* (CR 708). It becomes
     * foretold; you may look at it while it stays exiled. After the turn it was foretold has ended,
     * you may cast it from exile by paying its foretell cost rather than its mana cost.
     *
     * Structurally a paid cousin of [PLOT]: both exile from hand and cast-later-from-exile, but plot
     * is free to set up ({0}-ish printed cost) and free to cast later, whereas foretell always costs
     * {2} to exile and has a distinct per-card foretell cost to cast. The keyword itself is
     * display-only; cast/exile wiring lives in [com.wingedsheep.sdk.scripting.KeywordAbility.Foretell]
     * and the engine's foretell action handler + enumerator (which reuse the fixed-alternative-cost
     * cast-from-exile machinery that Airbend uses).
     */
    FORETELL("Foretell"),

    /**
     * Cleave [cost] (CR 702.148, Innistrad: Crimson Vow). Two static abilities that function while
     * a spell with cleave is on the stack (CR 702.148a): "You may cast this spell by paying [cost]
     * rather than paying its mana cost" and "If this spell's cleave cost was paid, change its text
     * by removing all text found within square brackets in the spell's rules text." The second
     * ability is a text-changing effect (CR 702.148b / 612).
     *
     * Modelled as an alternative cost ([KeywordAbility.Cleave]) whose paid branch swaps the spell's
     * effect and target requirements for a brackets-removed variant the card author supplies
     * explicitly ([com.wingedsheep.sdk.model.CardScript.cleaveSpellEffect] /
     * [com.wingedsheep.sdk.model.CardScript.cleaveTargetRequirements]) — a structural swap done at
     * cast time, not a cosmetic text edit, so e.g. a delayed triggered ability inside brackets is
     * never created at all (Alchemist's Gambit ruling). Cleave never changes mana value (CR 202.3b —
     * mana value is always computed from the printed mana cost). Wired by the `cleave(cost) { }` DSL
     * helper on [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    CLEAVE("Cleave"),

    /**
     * Daybound (CR 702.145, Innistrad: Midnight Hunt / Crimson Vow). Found on the **front** faces of
     * some transforming double-faced cards; represents three static abilities: "If it is night and
     * this permanent is represented by a transforming double-faced card, it enters transformed"; "As
     * it becomes night, if this permanent is front face up, transform it"; and "This permanent can't
     * transform except due to its daybound ability." Controlling a daybound permanent while it is
     * neither day nor night makes it day (CR 702.145d).
     *
     * Load-bearing, like [START_YOUR_ENGINES]: the engine reads this from projected state — the
     * [com.wingedsheep.engine.mechanics.daynight.DayNightService] transform cascade and the
     * `DayNightCheck` state-based sweep both scan for it — so a *granted* daybound works and no
     * per-card wiring beyond the keyword tag is needed. Add it with the `daybound()` helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder]. See [com.wingedsheep.sdk.core.DayNight].
     */
    DAYBOUND("Daybound"),

    /**
     * Nightbound (CR 702.145). Found on the **back** faces of the same transforming double-faced
     * cards; represents two static abilities: "As it becomes day, if this permanent is back face up,
     * transform it" and "This permanent can't transform except due to its nightbound ability."
     * Controlling a nightbound permanent while it is neither day nor night, with no daybound permanent
     * on the battlefield, makes it night (CR 702.145g).
     *
     * Load-bearing and read from projected state, exactly like [DAYBOUND]. Add it with the
     * `nightbound()` helper on [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    NIGHTBOUND("Nightbound"),

    // ── Creature mechanics ────────────────────────────────
    OFFSPRING("Offspring"),
    PERSIST("Persist"),
    UNDYING("Undying"),

    /**
     * Enduring (Duskmourn: House of Horror — the Glimmer "Enduring" cycle).
     * "When this permanent dies, if it was a creature, return it to the battlefield under its
     * owner's control. It's an enchantment. (It's not a creature.)"
     *
     * Modeled (like Persist) as a synthesized self-return triggered ability detected in
     * [com.wingedsheep.engine.event.DeathAndLeaveTriggerDetector]: it fires only when the
     * dying permanent was a creature (so the returned enchantment doesn't loop on its second
     * death) and is suppressed on tokens (CR 111.7 — tokens cease to exist). On return the
     * engine stamps an enduring-return marker; a [com.wingedsheep.sdk.scripting.ConditionalStaticAbility]
     * gated on that marker ([com.wingedsheep.sdk.scripting.conditions.SourceReturnedAsEnchantment])
     * makes the permanent an enchantment with no other card types or subtypes. Wired in one call
     * via the `enduring()` helper on [com.wingedsheep.sdk.dsl.CardBuilder]; the keyword itself is
     * display-only (no reminder badge beyond the printed text).
     */
    ENDURING("Enduring"),

    /**
     * Renew (Tarkir: Dragonstorm, Sultai clan keyword).
     * "Renew — [cost], Exile this card from your graveyard: [effect]. Activate only as a sorcery."
     *
     * A graveyard-activated ability composed of existing primitives: the mana cost plus
     * [com.wingedsheep.sdk.scripting.AbilityCost.ExileSelf], `activateFromZone = GRAVEYARD`,
     * and `timing = SorcerySpeed`. Wired in one call via the `renew(cost) { … }` helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder]; the keyword itself is display-only.
     */
    RENEW("Renew"),

    /**
     * Embalm [cost] (CR 702.128, Amonkhet).
     * "[Cost], Exile this card from your graveyard: Create a token that's a copy of it, except
     * it's a white Zombie with no mana cost. Activate only as a sorcery."
     *
     * Like [RENEW], a graveyard-activated ability composed of existing primitives — the mana cost
     * plus [com.wingedsheep.sdk.scripting.AbilityCost.ExileSelf], `activateFromZone = GRAVEYARD`,
     * `timing = SorcerySpeed` — whose effect is a
     * [com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect] of the card itself with
     * the three printed exceptions (white, +Zombie, no mana cost). Wired in one call via the
     * `embalm(cost)` helper on [com.wingedsheep.sdk.dsl.CardBuilder]; the keyword itself is
     * display-only.
     *
     * The same ability is what [com.wingedsheep.sdk.scripting.effects.GrantEmbalmEffect] hands to a
     * graveyard card at runtime ("target creature card in your graveyard gains embalm until end of
     * turn" — Cursecloth Wrappings), so printed and granted embalm are the same object.
     */
    EMBALM("Embalm"),

    /**
     * Ascend (Ixalan, CR 702.131). On a permanent spell, means "When this permanent
     * enters, if you control ten or more permanents, you get the city's blessing
     * for the rest of the game." Engine wires the trigger explicitly per card; the
     * keyword itself is only a textual marker for rules-text display.
     */
    ASCEND("Ascend"),

    /**
     * Storied (The Hobbit, CR 702.195a). A static ability: "Any time you control three or more
     * permanents that are artifacts, Sagas, and/or legendary and you don't have an enduring story,
     * you have an enduring story for the rest of the game."
     *
     * Load-bearing rather than display-only, and for the same reason as [START_YOUR_ENGINES]: the
     * engine's `StoriedEnduringStoryCheck` state-based action scans *projected* battlefield
     * permanents for this keyword, so granting storied at runtime works and stealing a storied
     * permanent hands the designation to its new controller. Add it with the `storied()` helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder]; read it back with
     * [com.wingedsheep.sdk.dsl.Conditions.YouHaveEnduringStory].
     *
     * Note the count is *not* the ascend count: three permanents each of which is an artifact, a
     * Saga, or legendary — one permanent satisfying two of those still counts once.
     */
    STORIED("Storied"),

    /**
     * Start your engines! (Aetherdrift, CR 702.179). "If a player controls a permanent with start
     * your engines! and that player has no speed, their speed becomes 1."
     *
     * Unlike most display-only keywords, this one is *load-bearing*: the engine's
     * `StartYourEnginesCheck` state-based action (CR 704.5aa) scans projected battlefield permanents
     * for this keyword, so granting it to a permanent at runtime works. Nothing else needs wiring on
     * the card — add it with the `startYourEngines()` helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder]. See [com.wingedsheep.sdk.core.Speed].
     */
    START_YOUR_ENGINES("Start your engines!"),

    /**
     * Max speed (Aetherdrift, CR 702.178). "Max speed — [Ability]" means "As long as your speed is
     * 4, this object has '[Ability].'"
     *
     * Display-only: the keyword prints the "Max speed — " prefix and drives the client badge, while
     * the gate itself is an ordinary condition applied to whatever ability the card grants. Author
     * it with the `maxSpeed { }` block on [com.wingedsheep.sdk.dsl.CardBuilder], which attaches this
     * keyword and gates each ability inside it on [com.wingedsheep.sdk.dsl.Conditions.YouHaveMaxSpeed].
     */
    MAX_SPEED("Max speed"),

    /**
     * Decayed (CR 702.147, Innistrad: Midnight Hunt). A static ability plus a
     * triggered ability: "This creature can't block" and "When this creature
     * attacks, sacrifice it at end of combat."
     *
     * The keyword itself is display-only; the behavior is composed by the
     * `decayed()` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder] — a
     * [com.wingedsheep.sdk.scripting.CantBlock] static ability plus an
     * attack-triggered [com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect]
     * that sacrifices the source at the end-of-combat step.
     */
    DECAYED("Decayed"),

    /**
     * Exploit (CR 702.110, Dragons of Tarkir). A triggered ability plus a paired payoff:
     * "When this creature enters, you may sacrifice a creature" (CR 702.110a). A creature
     * with exploit "exploits a creature" when its controller sacrifices a creature as that
     * ability resolves (CR 702.110b) — including sacrificing the exploiter itself.
     *
     * The keyword itself is display-only; the behavior is composed by the `exploit(onExploit)`
     * DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder] — an enters-the-battlefield
     * [com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect] ("you may sacrifice a
     * creature; when you do, …") whose reflexive emits an observable
     * [com.wingedsheep.sdk.scripting.EventPattern.ExploitedEvent] (so external watchers like
     * Skull Skaab can react) and then runs the optional self-payoff [onExploit].
     */
    EXPLOIT("Exploit"),

    /**
     * Training (CR 702.149, Innistrad: Midnight Hunt). A triggered attack ability:
     * "Whenever this creature and at least one other creature with power greater than this
     * creature's power attack, put a +1/+1 counter on this creature" (CR 702.149a). Multiple
     * instances trigger separately (CR 702.149b).
     *
     * The keyword itself is display-only; the behavior is composed by the `training()` DSL helper
     * on [com.wingedsheep.sdk.dsl.CardBuilder] — an attack-triggered ability
     * ([com.wingedsheep.sdk.dsl.Triggers.attacks] gated by
     * [com.wingedsheep.sdk.scripting.events.AttackPredicate.AttackedAlongsideGreaterPower], which
     * compares *projected* power across the attacking band) whose effect puts one +1/+1 counter on
     * the source ([com.wingedsheep.sdk.dsl.Effects.AddCounters]).
     */
    TRAINING("Training"),

    // ── Damage modification ──────────────────────────────
    WITHER("Wither"),
    TOXIC("Toxic"),

    // ── Numeric (parameterized by N) ──────────────────────
    ANNIHILATOR("Annihilator"),
    BUSHIDO("Bushido"),
    RAMPAGE("Rampage"),
    ABSORB("Absorb"),
    AFFLICT("Afflict"),
    CREW("Crew"),
    SADDLE("Saddle"),
    MODULAR("Modular"),
    FADING("Fading"),
    VANISHING("Vanishing"),

    /**
     * Suspend (CR 702.62). A card with suspend can be exiled with a number of time
     * counters on it. At the beginning of its owner's upkeep a time counter is removed,
     * and when the last is removed its owner plays it without paying its mana cost (with
     * haste, if it's a creature).
     *
     * The exile-side behavior is component-driven, not definition-driven: any exiled card
     * carrying the engine's suspended marker (set by [com.wingedsheep.sdk.dsl.Effects.Suspend]
     * or by the printed-suspend special action) gets the synthesized countdown-and-cast
     * triggered ability ([com.wingedsheep.sdk.scripting.Suspend.countdownAbility]). This
     * lets "exile it with N time counters; it gains suspend" effects (Taigam, Master
     * Opportunist) suspend an arbitrary card — even a card with no printed suspend — while a
     * printed "Suspend N—[cost]" (`KeywordAbility.Suspend`) drives the from-hand special
     * action (CR 116.2f) through the engine's `SuspendCardFromHandHandler`.
     */
    SUSPEND("Suspend"),
    RENOWN("Renown"),
    FABRICATE("Fabricate"),
    TRIBUTE("Tribute"),

    /**
     * Mobilize N (Tarkir: Dragonstorm, Mardu). "Whenever this creature attacks,
     * create N tapped and attacking 1/1 red Warrior creature tokens. Sacrifice
     * those tokens at the beginning of the next end step."
     *
     * The keyword ability is display-only; the behavior lives in an attack-triggered
     * ability wired by the `mobilize(n)` DSL helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder] — a [com.wingedsheep.sdk.scripting.effects.CreateTokenEffect]
     * that creates the tapped-and-attacking tokens and schedules their sacrifice at
     * the next end step via `sacrificeAtStep`.
     */
    MOBILIZE("Mobilize"),

    /**
     * Firebending N (Avatar: The Last Airbender). A numeric keyword ability:
     * "Whenever this creature attacks, add N {R}. Until end of combat, you don't
     * lose this mana as steps and phases end."
     *
     * Display-only on the keyword; the behavior is the attack-triggered ability
     * wired by the `firebending(n)` DSL helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder] — an
     * [com.wingedsheep.sdk.scripting.effects.AddManaEffect] producing red mana with
     * [com.wingedsheep.sdk.scripting.effects.ManaExpiry.END_OF_COMBAT] so the pool
     * keeps it through combat and discards it once combat ends.
     */
    FIREBENDING("Firebending"),

    /**
     * Job select (Final Fantasy). A keyword ability on Equipment:
     * "When this Equipment enters, create a 1/1 colorless Hero creature token, then
     * attach this to it."
     *
     * Display-only on the keyword; the behavior is the enters-the-battlefield triggered
     * ability wired by the `jobSelect()` DSL helper on
     * [com.wingedsheep.sdk.dsl.CardBuilder] — a
     * [com.wingedsheep.sdk.scripting.effects.CreateTokenEffect] that publishes the new
     * token's id to the `createdTokens` pipeline slot, followed by an
     * [com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect] that attaches the
     * source Equipment to that token.
     */
    JOB_SELECT("Job select"),

    // ── Ability words (display prefix, no uniform mechanic) ──
    /**
     * Eerie (Duskmourn: House of Horror).
     * Ability word — flavor prefix for effects that trigger whenever an enchantment
     * you control enters or whenever you fully unlock a Room.
     */
    EERIE("Eerie"),

    /**
     * Vivid (Lorwyn Eclipsed).
     * Ability word — flavor prefix for effects whose magnitude scales with the
     * number of distinct colors among permanents you control. No mechanical
     * behavior is attached to this keyword itself; each Vivid card still spells
     * out its own effect. Wired via the `vivid…` DSL helpers on [CardBuilder]
     * or by adding the appropriate effect/static ability directly.
     */
    VIVID("Vivid"),

    /**
     * Fateful Bite (Marvel's Spider-Man).
     * Ability word — flavor prefix used on Spider creatures whose activated abilities
     * tutor up other Spider-related cards. Per CR 207.2c, ability words have no rules
     * meaning; the prefix is metadata only and does not modify resolution.
     */
    FATEFUL_BITE("Fateful Bite"),

    /**
     * Prepared (Secrets of Strixhaven).
     * The printed "This creature enters prepared." line on a preparation card
     * ([com.wingedsheep.sdk.model.CardLayout.PREPARE]).
     *
     * **Load-bearing, not display-only**: a PREPARE-layout creature enters prepared if and only if
     * it carries this keyword — the stack resolver gates on layout *and* keyword. A card that only
     * *becomes* prepared later, via a trigger (Leech Collector) or an ETB conditional (Emeritus of
     * Truce), must omit it and use `Effects.BecomePrepared` instead. Scryfall tags `Prepared` on
     * *every* prepare-layout card regardless of the printed line, so its keyword list must never be
     * copied verbatim onto one of these.
     *
     * When the creature becomes prepared (however it got there), its controller creates a copy of
     * the card's prepare spell (`cardFaces[0]`) in exile that they may cast (paying that spell's
     * cost); casting the copy unprepares the creature.
     */
    PREPARED("Prepared"),

    /**
     * Paradigm (Secrets of Strixhaven).
     * Appears on Lesson spells. "Then exile this spell. After you first resolve a spell with this
     * name, you may cast a copy of it from exile without paying its mana cost at the beginning of
     * each of your first main phases." Display-only on the keyword — the behavior is driven by the
     * spell's `paradigm` flag, which routes the spell to exile on resolution and tags it with the
     * paradigm marker so the engine synthesizes the recurring free-recast ability
     * ([com.wingedsheep.sdk.scripting.Paradigm.recastAbility]).
     */
    PARADIGM("Paradigm"),

    /**
     * Increment (Secrets of Strixhaven).
     * "Whenever you cast a spell, if the amount of mana you spent is greater than this
     * creature's power or toughness, put a +1/+1 counter on this creature."
     * Wired via the `increment()` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder],
     * which attaches this display-only keyword plus the cast-spell triggered ability.
     */
    INCREMENT("Increment"),

    /**
     * Rebound (CR 702.88). "If this spell was cast from your hand, instead of putting it into
     * your graveyard as it resolves, exile it and, at the beginning of your next upkeep, you may
     * cast this card from exile without paying its mana cost." A static ability that functions
     * while the spell is on the stack (read by the spell-resolution path in `StackResolver`,
     * which honors both the printed keyword and one granted to the spell via
     * `GrantKeywordToSpellEffect` — Ojer Pakpatiq, Deepest Epoch grants it to instants you cast
     * from hand).
     */
    REBOUND("Rebound"),

    /**
     * Teamwork N (CR 702.194, Marvel Super Heroes). A static ability that functions while the
     * spell is on the stack: "As an additional cost to cast this spell, you may tap any number of
     * creatures you control with total power N or more" (CR 702.194a). A spell whose controller
     * declared that intention was cast *using teamwork* (CR 702.194b), and the card's own riders
     * branch on that fact.
     *
     * Modelled on the shared optional-additional-cost rail:
     * [com.wingedsheep.sdk.scripting.KeywordAbility.OptionalAdditionalCost] with
     * `declaredSlot = `[com.wingedsheep.sdk.scripting.ChoiceSlot.TEAMWORK], so "cast using
     * teamwork" is a *different* fact from "kicked" or "bargained". The cost itself is a
     * [com.wingedsheep.sdk.scripting.costs.CostAtom.VariablePermanents] tapping creatures measured
     * by total power — the same selection crew and saddle use. Wired by the `teamwork(n)` DSL
     * helper on [com.wingedsheep.sdk.dsl.CardBuilder]; payoffs read it back through
     * [com.wingedsheep.sdk.dsl.Conditions.TeamworkWasPaid].
     *
     * Tapping creatures this way is a *cost*, not the `{T}` symbol, so summoning sickness
     * (CR 302.6) never applies — only CR 701.26a's "only untapped permanents can be tapped".
     */
    TEAMWORK("Teamwork");

    companion object {
        fun fromString(value: String): Keyword? =
            entries.find { it.displayName.equals(value, ignoreCase = true) }

        fun parseFromOracleText(oracleText: String): Set<Keyword> {
            val keywords = mutableSetOf<Keyword>()
            val lines = oracleText.split("\n")

            for (line in lines) {
                val trimmed = line.trim()
                // Check for single keyword on a line (most common)
                fromString(trimmed)?.let { keywords.add(it) }

                // Check for comma-separated keywords (e.g., "Flying, vigilance")
                if (trimmed.contains(",")) {
                    trimmed.split(",").forEach { part ->
                        fromString(part.trim())?.let { keywords.add(it) }
                    }
                }

                // Check for ability word prefix: "Ability Word — effect description" (CR 207.2c)
                if (trimmed.contains('—')) {
                    val prefix = trimmed.substringBefore('—').trim()
                    fromString(prefix)?.let { keywords.add(it) }
                }
            }

            return keywords
        }
    }
}
