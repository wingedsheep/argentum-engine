package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invasion of Alara // Awaken the Maelstrom — March of the Machine #230.
 * {W}{U}{B}{R}{G} · Battle — Siege · defense 7 // Sorcery
 *
 * When this Siege enters, exile cards from the top of your library until you exile two nonland
 * cards with mana value 4 or less. You may cast one of those two cards without paying its mana
 * cost. Put one of them into your hand. Then put the other cards exiled this way on the bottom of
 * your library in a random order.
 * // Awaken the Maelstrom is all colors. Target player draws two cards. You may put an artifact
 * // card from your hand onto the battlefield. Create a token that's a copy of a permanent you
 * // control. Distribute three +1/+1 counters among one, two, or three creatures you control.
 * // Destroy target permanent an opponent controls.
 *
 * Front: `gatherUntilMatch(count = 2)` walks the library to the second hit; everything seen is
 * exiled. The cast is capped at one and resolves during the trigger. "Put one of them into your
 * hand" picks among the hits still in exile, and "the other cards exiled this way" are only the
 * misses — per the ruling, a hit that was neither cast nor handed stays in exile.
 *
 * Back: "is all colors" is a characteristic-defining ability (CR 604.3) on a face with no mana
 * cost, so it is carried by the face's colors — identical in every zone, as a CDA is. Only the
 * player and the destroyed permanent are targets; the artifact, the copied permanent and the
 * counter split are chosen on resolution, in printed order, so the token may copy the artifact
 * just put onto the battlefield. "Among one, two, or three" needs no floor: a creature given zero
 * counters simply isn't one of the chosen, and three counters can't reach a fourth.
 */
private val InvasionOfAlaraFront = card("Invasion of Alara") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Battle — Siege"
    startingDefense = 7
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, exile cards from the top of your library until you exile two " +
        "nonland cards with mana value 4 or less. You may cast one of those two cards without " +
        "paying its mana cost. Put one of them into your hand. Then put the other cards exiled " +
        "this way on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val (hits, seen) = gatherUntilMatch(
                GameObjectFilter.Nonland.manaValueAtMost(4),
                count = DynamicAmounts.fixed(2),
            )
            exile(seen)
            run(Effects.CastUpToNFromCollectionWithoutPayingCost(hits, 1))
            val uncast = filter(hits, GameObjectFilter.Any.currentlyIn(Zone.EXILE))
            val handed = chooseExactly(1, uncast, prompt = "Put one of them into your hand")
            toHand(handed)
            toLibraryBottom(exclude(seen, hits), order = CardOrder.Random)
        }
        description = "When this Siege enters, exile cards from the top of your library until you " +
            "exile two nonland cards with mana value 4 or less. You may cast one of those two cards " +
            "without paying its mana cost. Put one of them into your hand. Then put the other cards " +
            "exiled this way on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/318c363b-61cc-4e2f-8f86-a4287539ea07.jpg?1783951955"
        ruling("2023-04-14", "For Invasion of Alara's triggered ability, if you exile two nonland cards with mana value 4 or less, but you don't cast one of them, the one you don't put into your hand will remain in exile. It won't be put on the bottom of your library.")
        ruling("2023-04-14", "If you exile only one nonland card with mana value 4 or less, you'll have the option to cast it. If you don't, you'll put it into your hand.")
    }
}

private val AwakenTheMaelstrom = card("Awaken the Maelstrom") {
    manaCost = ""
    colorIdentity = "WUBRG"
    colorIndicator = "WUBRG"
    typeLine = "Sorcery"
    oracleText = "Awaken the Maelstrom is all colors.\n" +
        "Target player draws two cards. You may put an artifact card from your hand onto the " +
        "battlefield. Create a token that's a copy of a permanent you control. Distribute three " +
        "+1/+1 counters among one, two, or three creatures you control. Destroy target permanent " +
        "an opponent controls."

    spell {
        val drawer = target(Targets.Player)
        val victim = target(TargetFilter(GameObjectFilter.Permanent.opponentControls()))
        effect = Effects.DrawCards(2, drawer) then
            Patterns.Hand.putFromHand(GameObjectFilter.Artifact) then
            Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Permanent.youControl()) then
            Effects.DistributeCountersAmongFiltered(3, filter = GameObjectFilter.Creature.youControl()) then
            Effects.Destroy(victim)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/back/3/1/318c363b-61cc-4e2f-8f86-a4287539ea07.jpg?1783951955"
        ruling("2023-04-14", "Awaken the Maelstrom has two targets: the player who will draw cards and the permanent an opponent controls that will be destroyed. You must choose legal targets for both to cast Awaken the Maelstrom. All other choices are made on resolution. Specifically, this means you can create a token that's a copy of the artifact you just put onto the battlefield (which perhaps you just drew) and then put +1/+1 counters on it if it's also a creature.")
    }
}

val InvasionOfAlara: CardDefinition = CardDefinition.doubleFacedWithSpellBack(
    frontFace = InvasionOfAlaraFront,
    backFace = AwakenTheMaelstrom,
)
