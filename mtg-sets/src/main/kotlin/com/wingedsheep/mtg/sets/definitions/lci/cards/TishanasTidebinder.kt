package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tishana's Tidebinder
 * {2}{U}
 * Creature — Merfolk Wizard
 * 3/2
 *
 * Flash
 * When this creature enters, counter up to one target activated or triggered ability. If an ability
 * of an artifact, creature, or planeswalker is countered this way, that permanent loses all
 * abilities for as long as this creature remains on the battlefield. (Mana abilities can't be
 * targeted.)
 *
 * Modeling notes:
 *
 *  - **Flash** — [Keyword.FLASH]; the card can be cast any time you could cast an instant, so its
 *    ETB counter can answer an activated/triggered ability at instant speed.
 *
 *  - **"counter up to one target activated or triggered ability"** — an optional
 *    ([TargetObject] `optional = true`, i.e. "up to one") target over the on-stack
 *    activated/triggered abilities ([TargetFilter.ActivatedOrTriggeredAbilityOnStack]). Mana
 *    abilities never use the stack, so they are automatically excluded — the reminder text's
 *    "Mana abilities can't be targeted" is satisfied structurally. Declining the target (choosing
 *    none) makes the whole ability a no-op.
 *
 *  - **"If an ability of an artifact, creature, or planeswalker is countered this way, that
 *    permanent loses all abilities for as long as this creature remains on the battlefield"** —
 *    modeled as a `Composite` that runs the ability-strip **before** the counter, so the countered
 *    ability's source is still readable off its stack entity when we look it up (the counter then
 *    removes the ability from the stack):
 *      * [Effects.RemoveAbilitiesFromSourceOfTargetedAbility] finds the targeted ability's source
 *        permanent; if it is currently an artifact, creature, or planeswalker on the battlefield
 *        (`sourceCardTypes`), it applies a Layer-6 "loses all abilities" floating effect keyed to
 *        Tishana via [Duration.WhileSourceOnBattlefield]. If the source has already left the
 *        battlefield, or is (say) an enchantment or land, no strip happens.
 *      * [Effects.CounterAbility] then counters the targeted ability.
 *    Because the strip's floating effect is source-keyed to Tishana, it ends the moment Tishana
 *    leaves the battlefield (`EndedDurationExpiryCheck`), restoring the permanent's abilities.
 *
 * Earliest (and only) printing is The Lost Caverns of Ixalan (2023); this is the canonical
 * definition.
 */
val TishanasTidebinder = card("Tishana's Tidebinder") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "Flash\n" +
        "When this creature enters, counter up to one target activated or triggered ability. If an " +
        "ability of an artifact, creature, or planeswalker is countered this way, that permanent " +
        "loses all abilities for as long as this creature remains on the battlefield. (Mana " +
        "abilities can't be targeted.)"
    power = 3
    toughness = 2

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield

        target(
            "up to one target activated or triggered ability",
            TargetObject(
                filter = TargetFilter.ActivatedOrTriggeredAbilityOnStack,
                optional = true
            )
        )

        // Strip first (source still readable off the stack entity), then counter.
        effect = Effects.Composite(
            Effects.RemoveAbilitiesFromSourceOfTargetedAbility(
                duration = Duration.WhileSourceOnBattlefield("this creature"),
                sourceCardTypes = setOf(CardType.ARTIFACT, CardType.CREATURE, CardType.PLANESWALKER)
            ),
            Effects.CounterAbility()
        )
        description = "When this creature enters, counter up to one target activated or triggered " +
            "ability. If an ability of an artifact, creature, or planeswalker is countered this " +
            "way, that permanent loses all abilities for as long as this creature remains on the " +
            "battlefield. (Mana abilities can't be targeted.)"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Nino Vecia"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/907b3d1d-8c85-4707-80b5-c4d832df9846.jpg?1782694545"
    }
}
