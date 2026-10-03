package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Boggart Trawler {2}{B} // Boggart Bog
 * Creature — Goblin 3/1
 * When this creature enters, exile target player's graveyard.
 * //
 * Land
 * As this land enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {B}.
 *
 * The whole-graveyard exile is the Angel of Finality gather-then-move over the targeted
 * player's graveyard.
 */
private val BoggartTrawlerFront = card("Boggart Trawler") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Goblin"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, exile target player's graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(Targets.Player)
        effect = Effects.Pipeline {
            val targetGraveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.TargetPlayer))
            exile(targetGraveyard)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Randy Gallegos"
        flavorText = "\"Look what I found, and it's only been dead for a week!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0d484a6-5610-4f1d-95ec-eda273c255e4.jpg?1783911234"
    }
}

private val BoggartBogBack = card("Boggart Bog") {
    typeLine = "Land"
    colorIdentity = "B"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {B}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Randy Gallegos"
        flavorText = "A boggart warren is usually located in a place rich with sensations... good and bad."
        imageUri = "https://cards.scryfall.io/normal/back/d/0/d0d484a6-5610-4f1d-95ec-eda273c255e4.jpg?1783911234"
    }
}

val BoggartTrawler: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = BoggartTrawlerFront,
    backFace = BoggartBogBack,
)
