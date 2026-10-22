package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Poppet Stitcher // Poppet Factory (Innistrad: Midnight Hunt #71)
 *
 * Front — Poppet Stitcher ({2}{U}, Creature — Human Wizard, 2/3)
 *   Whenever you cast an instant or sorcery spell, create a 2/2 black Zombie creature token with
 *   decayed.
 *   At the beginning of your upkeep, if you control three or more creature tokens, you may
 *   transform this creature.
 *
 * Back — Poppet Factory (Artifact, blue color indicator)
 *   Creature tokens you control lose all abilities and have base power and toughness 3/3.
 *   At the beginning of your upkeep, you may transform this artifact.
 *
 * The upkeep check is an intervening "if": it must hold both when the trigger would fire and on
 * resolution. The Factory's statics apply in Layer 6 (lose all abilities) and Layer 7b (base P/T);
 * the engine realizes Decayed off the projected keyword set, so stripped tokens can block and
 * aren't sacrificed for attacking. A token that already attacked while it had decayed is still
 * sacrificed at end of combat (the delayed trigger was already created).
 */
private val PoppetStitcherFront = card("Poppet Stitcher") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast an instant or sorcery spell, create a 2/2 black Zombie creature " +
        "token with decayed. (It can't block. When it attacks, sacrifice it at end of combat.)\n" +
        "At the beginning of your upkeep, if you control three or more creature tokens, you may " +
        "transform this creature."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.DECAYED),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouControlAtLeast(3, GameObjectFilter.Creature.token())
        effect = Effects.May(Effects.Transform(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "71"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/999038b3-7d64-4554-b341-0675d4af8d8b.jpg?1783925638"
        ruling("2021-09-24", "Once a creature with decayed attacks, it will be sacrificed at end of combat, even if it no longer has decayed at that time.")
    }
}

private val PoppetFactory = card("Poppet Factory") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Artifact"
    oracleText = "Creature tokens you control lose all abilities and have base power and toughness 3/3.\n" +
        "At the beginning of your upkeep, you may transform this artifact."

    val creatureTokensYouControl = GroupFilter(GameObjectFilter.Creature.token().youControl())

    staticAbility { ability = LoseAllAbilities(filter = creatureTokensYouControl) }
    staticAbility {
        ability = SetBasePowerToughnessStatic(power = 3, toughness = 3, filter = creatureTokensYouControl)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(Effects.Transform(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "71"
        artist = "Simon Dominic"
        flavorText = "It may have been a mistake to give them minds of their own."
        imageUri = "https://cards.scryfall.io/normal/back/9/9/999038b3-7d64-4554-b341-0675d4af8d8b.jpg?1783925638"
    }
}

val PoppetStitcher: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = PoppetStitcherFront,
    backFace = PoppetFactory,
)
