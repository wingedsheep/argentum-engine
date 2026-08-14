package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Thing, Ben Grimm — Marvel Super Heroes #190 (uncommon)
 * {5}{G} · Legendary Creature — Human Hero · 7/7
 *
 * Trample
 * Whenever one or more Heroes you control deal damage to a player, put two +1/+1 counters on
 * The Thing.
 *
 * Implementation notes:
 * - This is a **source-side** damage trigger: the observed object is the damage *source* (a Hero
 *   you control), not the recipient. That is [Triggers.dealsDamage] with a `sourceFilter` of
 *   `Creature.youControl().withSubtype(HERO)` and `recipient = RecipientFilter.AnyPlayer`, on
 *   [TriggerBinding.ANY] so every Hero you control is watched — The Thing is itself a Hero, so its
 *   own damage counts.
 * - `batch = true` gives the printed "one or more … deal damage" wording (CR 603.2c): three Heroes
 *   connecting in the same combat damage step is one trigger, not three.
 * - `DamageType.Any` (the default) is deliberate. The printed text is not combat-restricted, so a
 *   Hero pinging a player with an activated ability triggers it too; the combat-only
 *   `OneOrMoreDealCombatDamageToPlayerEvent` sugar would silently drop that half of the card.
 * - The auto-generated trigger text for a `dealsDamage` pattern is written from the recipient's
 *   side ("whenever … is dealt damage"), which reads backwards here, so the ability carries an
 *   explicit [description].
 *
 * Known semantic edge — `DamageTriggerDetector.detectDamageObserverBatchTriggers` fires a batch
 * observer at most **once per event batch**, not once per distinct trigger event. Several Heroes
 * damaging *one* player simultaneously therefore fires once (correct), but Heroes damaging *two
 * different* players simultaneously — e.g. two attackers assigned to two opponents in a multiplayer
 * game, or a Hero with an "each opponent" damage ability — also fires only once, where CR 603.2c
 * wants one trigger per damaged player. Closing that needs a per-recipient batch grouping in the
 * detector, which is an `add-feature` change rather than a card change; in the two-player games the
 * engine is exercised with, the two readings coincide.
 */
val TheThingBenGrimm = card("The Thing, Ben Grimm") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Hero"
    power = 7
    toughness = 7
    oracleText = "Trample\n" +
        "Whenever one or more Heroes you control deal damage to a player, put two +1/+1 counters " +
        "on The Thing."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            recipient = RecipientFilter.AnyPlayer,
            sourceFilter = GameObjectFilter.Creature.youControl().withSubtype(Subtype.HERO),
            binding = TriggerBinding.ANY,
            batch = true,
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        description = "Whenever one or more Heroes you control deal damage to a player, put two " +
            "+1/+1 counters on The Thing."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Gintas Galvanauskas"
        flavorText = "\"Don't worry, pal! Soon all yer problems'll be over, 'cause it's " +
            "clobberin' time!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f1933a8-046f-471a-afcf-cfb08ca0d239.jpg?1783902911"
    }
}
