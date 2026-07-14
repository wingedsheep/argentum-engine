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

    // ── ETB modification ──────────────────────────────────────
    AMPLIFY("Amplify"),

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
     * Miracle {cost} (CR 702.94). "You may cast this card for its miracle cost when you draw it if
     * it's the first card you drew this turn." Modeled as a hand-only alternative cost gated by a
     * one-turn window: when a card with miracle (printed via
     * [com.wingedsheep.sdk.scripting.KeywordAbility.Miracle] or granted via
     * [com.wingedsheep.sdk.scripting.GrantMiracleToCardsInHand]) is the first card a player draws in
     * a turn, the engine stamps it with a miracle window for that turn; the cast-from-hand
     * enumerator then surfaces a "Cast (Miracle)" alternative cost at the miracle mana cost.
     */
    MIRACLE("Miracle"),
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

    // ── Creature mechanics ────────────────────────────────
    OFFSPRING("Offspring"),
    PERSIST("Persist"),

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
     * Ascend (Ixalan, CR 702.131). On a permanent spell, means "When this permanent
     * enters, if you control ten or more permanents, you get the city's blessing
     * for the rest of the game." Engine wires the trigger explicitly per card; the
     * keyword itself is only a textual marker for rules-text display.
     */
    ASCEND("Ascend"),

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
     * The keyword is display-only. The exile-side behavior is component-driven, not
     * definition-driven: any exiled card carrying the engine's suspended marker (set by
     * [com.wingedsheep.sdk.dsl.Effects.Suspend]) gets the synthesized countdown-and-cast
     * triggered ability ([com.wingedsheep.sdk.scripting.Suspend.countdownAbility]). This
     * lets "exile it with N time counters; it gains suspend" effects (Taigam, Master
     * Opportunist) suspend an arbitrary card — even a card with no printed suspend.
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
     * Display keyword on a preparation card ([com.wingedsheep.sdk.model.CardLayout.PREPARE]):
     * "This creature enters prepared." Display-only on the keyword — the behavior is driven by
     * the PREPARE layout. When the creature becomes prepared (e.g. as it enters), its controller
     * creates a copy of the card's prepare spell (`cardFaces[0]`) in exile that they may cast
     * (paying that spell's cost); casting the copy unprepares the creature.
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
    INCREMENT("Increment");

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
