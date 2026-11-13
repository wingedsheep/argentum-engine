package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val KIORA_EMBLEM = "Whenever a creature you control enters, you may have it fight target creature."

/**
 * Kiora, Master of the Depths - {2}{G}{U}
 * Legendary Planeswalker — Kiora
 * Starting Loyalty: 4
 *
 * +1: Untap up to one target creature and up to one target land.
 * −2: Reveal the top four cards of your library. You may put a creature card and/or a land card
 *     from among them into your hand. Put the rest into your graveyard.
 * −8: You get an emblem with "Whenever a creature you control enters, you may have it fight
 *     target creature." Then create three 8/8 blue Octopus creature tokens.
 *
 * The +1 is Teferi, Who Slows the Sunset's "up to one target X and up to one target Y" shape
 * (untapping whatever was actually chosen, so either slot may be left empty); the −2 is Benefaction
 * of Rhonas's reveal-and-split pipeline with a land pick in place of the enchantment pick; the −8
 * emblem is Frontier Siege's "may have it fight target creature" trigger as a global emblem
 * trigger (Koth, Fire of Resistance's shape). The emblem exists before the Octopuses are created,
 * so each of them triggers it.
 */
val KioraMasterOfTheDepths = card("Kiora, Master of the Depths") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Kiora"
    startingLoyalty = 4
    oracleText = "+1: Untap up to one target creature and up to one target land.\n" +
        "−2: Reveal the top four cards of your library. You may put a creature card and/or a land card " +
        "from among them into your hand. Put the rest into your graveyard.\n" +
        "−8: You get an emblem with \"$KIORA_EMBLEM\" Then create three 8/8 blue Octopus creature tokens."

    // +1: Untap up to one target creature and up to one target land.
    loyaltyAbility(+1) {
        target(TargetFilter.Creature, optional = true)
        target(TargetFilter.Land, optional = true)
        effect = Effects.Pipeline(
            descriptionOverride = "Untap up to one target creature and up to one target land."
        ) {
            val chosen = gather(CardSource.ChosenTargets)
            run(Effects.TapCollection(chosen, tap = false))
        }
        description = "Untap up to one target creature and up to one target land."
    }

    // −2: Reveal the top four cards of your library. You may put a creature card and/or a land card
    //     from among them into your hand. Put the rest into your graveyard.
    loyaltyAbility(-2) {
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(4), revealed = true)
            val (creature, afterCreature) = chooseUpToSplit(
                1, from = revealed,
                filter = GameObjectFilter.Creature,
                prompt = "You may put a creature card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave",
                showAllCards = true
            )
            val (land, rest) = chooseUpToSplit(
                1, from = afterCreature,
                filter = GameObjectFilter.Land,
                prompt = "You may put a land card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put into graveyard",
                showAllCards = true
            )
            toHand(creature)
            toHand(land)
            toGraveyard(rest)
        }
        description = "Reveal the top four cards of your library. You may put a creature card and/or a land " +
            "card from among them into your hand. Put the rest into your graveyard."
    }

    // −8: You get an emblem with "Whenever a creature you control enters, you may have it fight
    //     target creature." Then create three 8/8 blue Octopus creature tokens.
    loyaltyAbility(-8) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
                val foe = target(TargetFilter.Creature)
                optional = true
                effect = Effects.Fight(EffectTarget.TriggeringEntity, foe)
                description = KIORA_EMBLEM
            },
            emblem = true,
            descriptionOverride = KIORA_EMBLEM,
        ) then Effects.CreateToken(
            power = 8,
            toughness = 8,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Octopus"),
            count = 3,
            imageUri = "https://cards.scryfall.io/normal/front/1/9/19ac0a35-fae7-49f9-ae96-4406df992dc9.jpg?1783938120",
        )
        description = "You get an emblem with \"$KIORA_EMBLEM\" Then create three 8/8 blue Octopus creature tokens."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "213"
        artist = "Jason Chan"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b63c228-3265-4029-9bc0-36b3e31dfb53.jpg?1783938179"
        ruling("2015-08-25", "You can activate Kiora's first ability with no targets just to put a loyalty counter on her.")
        ruling("2015-08-25", "With Kiora's second ability, you could put no cards, a creature card, a land card, or a creature card and a land card into your hand.")
        ruling("2015-08-25", "You will have the emblem by the time the Octopus creature tokens enter the battlefield—they'll be looking to fight!")
    }
}
