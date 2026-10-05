package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pirated Copy — "this creature or another creature with the same name" is a single observer
 * trigger over every creature, narrowed by a CR 603.2 trigger restriction that the damage source
 * shares this permanent's (copied) name. The restriction runs with the trigger's own source in
 * context, so `EffectTarget.Self` resolves to the copy; the trigger-subject filter does not carry
 * a source, which is why the name check isn't on the subject.
 */
val PiratedCopy = card("Pirated Copy") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Shapeshifter Pirate"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of any creature on the battlefield, except it's a Pirate in addition to its other types and it has \"Whenever this creature or another creature with the same name deals combat damage to a player, you draw a card.\""

    replacementEffect(EntersAsCopy(
        exceptions = CopyExceptions(
            addedSubtypes = setOf(Subtype.PIRATE),
            addedTriggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.a(GameObjectFilter.Creature).dealsCombatDamage(Recipient.AnyPlayer)
                    triggerRestriction = Conditions.EntityMatches(
                        EffectTarget.TriggeringEntity,
                        GameObjectFilter.Creature.sharingNameWith(EffectTarget.Self)
                    )
                    effect = Effects.DrawCards(1)
                }
            )
        )
    ))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "16"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a0567fb-2b4f-4fa9-8dfe-df4e665a4c5c.jpg?1783919190"
        ruling("2022-12-02", "Being a Pirate and having the granted ability are part of the copiable values of Pirated Copy. If another creature enters the battlefield as or becomes a copy of Pirated Copy, it will copy whatever Pirated Copy is copying, be a Pirate in addition to its other types, and have \"Whenever this creature or another creature with the same name deals combat damage to a player, you draw a card.\"")
        ruling("2022-12-02", "If Pirated Copy doesn't enter the battlefield as a creature (such as by copying an artifact or land that became a creature), it doesn't become a Pirate, though it still has the granted triggered ability. It won't become a Pirate even if it later turns into a creature from another effect.")
        ruling("2022-12-02", "If the chosen creature is copying something else, then Pirated Copy enters the battlefield as whatever the chosen creature is copying. It will also be a Pirate and have the granted ability.")
        ruling("2022-12-02", "If Pirated Copy somehow enters the battlefield at the same time as another creature, Pirated Copy can't become a copy of that creature. You may choose only a creature that's already on the battlefield.")
        ruling("2022-12-02", "You can choose to not copy anything. In that case, Pirated Copy enters the battlefield as a 0/0 Shapeshifter Pirate creature, and will probably die almost immediately, when state-based actions are next performed.")
    }
}
