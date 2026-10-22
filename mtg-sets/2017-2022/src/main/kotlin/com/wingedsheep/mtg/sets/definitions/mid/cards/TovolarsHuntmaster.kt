package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Tovolar's Huntmaster // Tovolar's Packleader (Innistrad: Midnight Hunt)
 * {4}{G}{G}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front (6/6): When this creature enters, create two 2/2 green Wolf creature tokens. Daybound.
 * Back (7/7): Whenever this creature enters or attacks, create two 2/2 green Wolf creature tokens.
 * {2}{G}{G}: Another target Wolf or Werewolf you control fights target creature you don't control.
 * Nightbound.
 *
 * "Enters or attacks" is two triggered abilities sharing one effect (the Grave Titan shape). The back
 * face's enters trigger fires when the card enters at night (nightbound side up).
 */
private fun twoWolves() = Effects.CreateToken(
    power = 2,
    toughness = 2,
    colors = setOf(Color.GREEN),
    creatureTypes = setOf("Wolf"),
    count = 2,
)

private val TovolarsHuntmasterFront = card("Tovolar's Huntmaster") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 6
    toughness = 6
    oracleText = "When this creature enters, create two 2/2 green Wolf creature tokens.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = twoWolves()
        description = "When this creature enters, create two 2/2 green Wolf creature tokens."
    }

    daybound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Cristi Balanescu"
        flavorText = "\"Do you smell that, friends? Fear and despair.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/9/3983a304-5040-4b8d-945a-bf4ede3104a8.jpg?1783925575"
    }
}

private val TovolarsPackleader = card("Tovolar's Packleader") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 7
    toughness = 7
    oracleText = "Whenever this creature enters or attacks, create two 2/2 green Wolf creature tokens.\n" +
        "{2}{G}{G}: Another target Wolf or Werewolf you control fights target creature you don't control.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    val tokenDescription = "Whenever this creature enters or attacks, create two 2/2 green Wolf creature tokens."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = twoWolves()
        description = tokenDescription
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = twoWolves()
        description = tokenDescription
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}{G}")
        val mine = target(
            TargetFilter(
                GameObjectFilter.Permanent.withAnySubtype("Wolf", "Werewolf").youControl(),
                excludeSelf = true,
            )
        )
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.Fight(mine, theirs)
        description = "{2}{G}{G}: Another target Wolf or Werewolf you control fights target creature you don't control."
    }

    nightbound()

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/back/3/9/3983a304-5040-4b8d-945a-bf4ede3104a8.jpg?1783925575"
    }
}

val TovolarsHuntmaster: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = TovolarsHuntmasterFront,
    backFace = TovolarsPackleader,
)
