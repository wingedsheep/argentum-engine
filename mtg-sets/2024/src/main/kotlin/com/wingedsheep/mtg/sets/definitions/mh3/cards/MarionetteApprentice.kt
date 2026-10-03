package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marionette Apprentice — Modern Horizons 3 #100
 * {1}{B} · Creature — Human Artificer · 1/2 · Uncommon
 *
 * Fabricate 1 (When this creature enters, put a +1/+1 counter on it or create a 1/1 colorless
 * Servo artifact creature token.)
 * Whenever another creature or artifact you control is put into a graveyard from the
 * battlefield, each opponent loses 1 life.
 */
val MarionetteApprentice = card("Marionette Apprentice") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Artificer"
    power = 1
    toughness = 2
    oracleText = "Fabricate 1 (When this creature enters, put a +1/+1 counter on it or create a 1/1 " +
        "colorless Servo artifact creature token.)\n" +
        "Whenever another creature or artifact you control is put into a graveyard from the " +
        "battlefield, each opponent loses 1 life."

    keywordAbility(KeywordAbility.fabricate(1))

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.CreatureOrArtifact.youControl()).dies()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever another creature or artifact you control is put into a graveyard " +
            "from the battlefield, each opponent loses 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Steve Ellis"
        flavorText = "\"Ah! Yes! I got its leg to twitch!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d16f8670-f038-400a-83e7-a53a7f8c47ac.jpg?1783911278"
        ruling(
            "2024-06-07",
            "If you can't put a +1/+1 counter on the creature for any reason as fabricate resolves " +
                "(for instance, if it's no longer on the battlefield), you just create a Servo token."
        )
    }
}
