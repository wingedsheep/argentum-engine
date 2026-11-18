package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deicide — Journey into Nyx #7
 * {1}{W} · Instant
 *
 * Exile target enchantment. If the exiled card is a God card, search its controller's graveyard,
 * hand, and library for any number of cards with the same name as that card and exile them. Then
 * that player shuffles.
 *
 * The God check reads the card *in exile* (the ruling): `moveTracked` keeps only what actually
 * reached exile, and the condition matches the base card there, so a Theros God that wasn't a
 * creature for lack of devotion still counts — and a token, which isn't a card, never does. "Its
 * controller" is the enchantment's last-known controller, read after it has left the battlefield.
 * The search, the "any number" pick and the shuffle all sit inside the God branch: "that player"
 * only exists once the search is under way.
 */
val Deicide = card("Deicide") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target enchantment. If the exiled card is a God card, search its controller's " +
        "graveyard, hand, and library for any number of cards with the same name as that card and exile " +
        "them. Then that player shuffles."

    spell {
        target(TargetFilter.Enchantment)
        val thatPlayer = Player.ControllerOf("target enchantment")
        effect = Effects.Pipeline {
            val targetCard = gather(CardSource.ChosenTargets)
            val cardName = storeCardName(targetCard)
            val exiled = moveTracked(targetCard, CardDestination.ToZone(Zone.EXILE))
            ifThen(Conditions.CollectionContainsMatch(exiled, GameObjectFilter.Any.withSubtype("God").nontoken())) {
                val matches = gather(
                    CardSource.FromMultipleZones(
                        zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                        player = thatPlayer,
                        filter = GameObjectFilter.Any.namedFromVariable(cardName)
                    ),
                    search = true
                )
                val toExile = chooseAnyNumber(
                    from = matches,
                    prompt = "Choose any number of cards with that name to exile",
                    selectedLabel = "Exile",
                    showAllCards = true,
                    alwaysPrompt = true
                )
                exile(toExile, owner = thatPlayer)
                run(Effects.ShuffleLibrary(target = EffectTarget.PlayerRef(thatPlayer)))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Jason Chan"
        flavorText = "\"It is done.\"\n—Elspeth"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/6012964c-eb76-4581-82ae-aec2d36f0d56.jpg?1783939462"
        ruling(
            "2014-04-26",
            "Deicide looks at the card in exile, not the permanent that was exiled, to determine if it is a God. " +
                "For each of the Gods in the Theros block, it won't matter what your devotion to its color(s) was. " +
                "The card is a God card when not on the battlefield."
        )
        ruling(
            "2014-04-26",
            "When you search the target's controller's graveyard, hand, and library, you may leave any cards with " +
                "that name in the zones they're in. You don't have to exile them."
        )
    }
}
