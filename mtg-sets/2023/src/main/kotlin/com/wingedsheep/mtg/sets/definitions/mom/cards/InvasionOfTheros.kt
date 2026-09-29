package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Invasion of Theros // Ephara, Ever-Sheltering — March of the Machine #23.
 * {2}{W} · Battle — Siege · defense 4 // Legendary Enchantment Creature — God 4/4
 *
 * Front: the Siege's enter trigger tutors an Aura, God, or Demigod card to hand (revealed).
 * Back (white and blue): lifelink and indestructible while you control three or more *other*
 * enchantments — the tally excludes Ephara herself — and a card whenever another enchantment you
 * control enters.
 */
private val InvasionOfTherosFront = card("Invasion of Theros") {
    manaCost = "{2}{W}"
    colorIdentity = "WU"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, search your library for an Aura, God, or Demigod card, reveal " +
        "it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withAnySubtype("Aura", "God", "Demigod"),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
        )
        description = "When this Siege enters, search your library for an Aura, God, or Demigod " +
            "card, reveal it, put it into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/433e9f1c-9d6c-4c7e-89d0-79595b4331f2.jpg?1783917071"
    }
}

private val EpharaEverSheltering = card("Ephara, Ever-Sheltering") {
    manaCost = ""
    colorIdentity = "WU"
    colorIndicator = "WU"
    typeLine = "Legendary Enchantment Creature — God"
    power = 4
    toughness = 4
    oracleText = "Ephara has lifelink and indestructible as long as you control at least three " +
        "other enchantments.\n" +
        "Whenever another enchantment you control enters, draw a card."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.LIFELINK, GroupFilter.source()),
            condition = Conditions.YouControlOtherAtLeast(3, GameObjectFilter.Enchantment),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, GroupFilter.source()),
            condition = Conditions.YouControlOtherAtLeast(3, GameObjectFilter.Enchantment),
        )
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Enchantment.youControl()).enters()
        effect = Effects.DrawCards(1)
        description = "Whenever another enchantment you control enters, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Johan Grenier"
        flavorText = "When the sun falters and the seas disperse, when the wilds wither and the " +
            "forges go cold, when death itself succumbs, she endures."
        imageUri = "https://cards.scryfall.io/normal/back/4/3/433e9f1c-9d6c-4c7e-89d0-79595b4331f2.jpg?1783917071"
        ruling("2023-04-14", "Damage dealt to Ephara is tracked even if Ephara has indestructible. For example, if Ephara is dealt what would be lethal damage and later loses indestructible (perhaps because you lose control of some enchantments), it will be destroyed the next time state-based actions are performed. However, the check for whether a creature dealt damage by a source with deathtouch is destroyed happens only the first time that state-based actions are performed after that damage-dealing event.")
        ruling("2023-04-14", "If Ephara enters the battlefield under your control at the same time as other enchantments, its last ability will trigger for each of those other enchantments.")
    }
}

val InvasionOfTheros: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfTherosFront,
    backFace = EpharaEverSheltering,
)
