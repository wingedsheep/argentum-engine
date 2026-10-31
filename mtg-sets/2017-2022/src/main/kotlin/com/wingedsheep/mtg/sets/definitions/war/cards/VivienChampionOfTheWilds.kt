package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vivien, Champion of the Wilds — War of the Spark #180 (canonical printing)
 * {2}{G} · Legendary Planeswalker — Vivien · Starting Loyalty: 4
 *
 * You may cast creature spells as though they had flash.
 * +1: Until your next turn, up to one target creature gains reach and vigilance.
 * −2: Look at the top three cards of your library. Exile one face down and put the rest on the
 *     bottom of your library in any order. For as long as it remains exiled, you may cast it if
 *     it's a creature spell.
 *
 * The −2 always exiles a card (first ruling), which stays lookable whatever it is. "If it's a
 * creature spell" is the permission's spell-type rider, checked against the face being cast: an
 * exiled Adventurer card's Adventure is an instant or sorcery spell, so only its creature half can
 * be cast. The permission outlives Vivien, but the flash timing comes from her static, so without
 * her the creature is cast at sorcery speed (second ruling).
 */
val VivienChampionOfTheWilds = card("Vivien, Champion of the Wilds") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Vivien"
    startingLoyalty = 4
    oracleText = "You may cast creature spells as though they had flash.\n" +
        "+1: Until your next turn, up to one target creature gains reach and vigilance.\n" +
        "−2: Look at the top three cards of your library. Exile one face down and put the rest on the " +
        "bottom of your library in any order. For as long as it remains exiled, you may cast it if " +
        "it's a creature spell."

    staticAbility {
        ability = GrantFlashToSpellType(filter = GameObjectFilter.Creature, controllerOnly = true)
    }

    loyaltyAbility(+1) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.GrantKeyword(Keyword.REACH, creature, Duration.UntilYourNextTurn) then
            Effects.GrantKeyword(Keyword.VIGILANCE, creature, Duration.UntilYourNextTurn)
        description = "Until your next turn, up to one target creature gains reach and vigilance."
    }

    loyaltyAbility(-2) {
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(3))
            val split = chooseExactlySplit(
                1,
                from = top,
                selectedLabel = "Exile face down",
                remainderLabel = "Put on the bottom of your library"
            )
            move(
                split.selected,
                CardDestination.ToZone(Zone.EXILE),
                faceDown = FaceDownMode.HIDDEN,
                lookableInExile = true
            )
            toLibraryBottom(split.remainder)
            run(
                Effects.GrantMayPlayFromExile(
                    split.selected,
                    expiry = MayPlayExpiry.Permanent,
                    nonLandOnly = true,
                    castSpellTypes = setOf(CardType.CREATURE)
                )
            )
        }
        description = "Look at the top three cards of your library. Exile one face down and put the rest " +
            "on the bottom of your library in any order. For as long as it remains exiled, you may cast " +
            "it if it's a creature spell."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff3986d7-9b3d-4082-8d72-ce59c9fcd5d5.jpg?1783933404"
        ruling(
            "2019-05-03",
            "While resolving Vivien's last ability, you must exile one of the three cards, even if none of " +
                "them are creature cards. You may look at the exiled card at any time, even if it's not a " +
                "creature card."
        )
        ruling(
            "2019-05-03",
            "You must follow any applicable timing permissions and restrictions if you cast the exiled card. " +
                "This means that if Vivien leaves the battlefield, you can't cast the creature spell as though " +
                "it had flash unless another effect allows you to do so."
        )
        ruling(
            "2019-05-03",
            "Once you cast the creature card, it leaves exile. You can't cast it again if that card becomes " +
                "exiled again."
        )
    }
}
