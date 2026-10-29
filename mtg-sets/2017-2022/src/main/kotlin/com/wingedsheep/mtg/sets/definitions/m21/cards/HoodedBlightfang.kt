package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hooded Blightfang — Core Set 2021 #104 (canonical printing)
 * {2}{B} · Creature — Snake · 1/4
 *
 * Deathtouch
 * Whenever a creature you control with deathtouch attacks, each opponent loses 1 life and you
 * gain 1 life.
 * Whenever a creature you control with deathtouch deals damage to a planeswalker, destroy that
 * planeswalker.
 *
 * Both triggers are observer triggers whose source filter reads deathtouch off projected state, so
 * creatures that gained deathtouch count (Fynn, the Fangbearer's shape). The gain is a flat 1, not
 * "the life lost this way" — the Two-Headed Giant ruling has the team lose 2 and you gain 1. The
 * damage trigger is any damage, not just combat (Zagras's trigger without the combat restriction);
 * "that planeswalker" is [EffectTarget.DamageRecipient].
 */
val HoodedBlightfang = card("Hooded Blightfang") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Snake"
    power = 1
    toughness = 4
    oracleText = "Deathtouch\n" +
        "Whenever a creature you control with deathtouch attacks, each opponent loses 1 life and you gain 1 life.\n" +
        "Whenever a creature you control with deathtouch deals damage to a planeswalker, destroy that planeswalker."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withKeyword(Keyword.DEATHTOUCH)).attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
        description = "Whenever a creature you control with deathtouch attacks, each opponent loses 1 life " +
            "and you gain 1 life."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withKeyword(Keyword.DEATHTOUCH))
            .dealsDamage(Recipient.Object(GameObjectFilter.Planeswalker))
        effect = Effects.Destroy(EffectTarget.DamageRecipient)
        description = "Whenever a creature you control with deathtouch deals damage to a planeswalker, " +
            "destroy that planeswalker."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac38a51f-9a3b-451c-b72d-6d4e0b296fbd.jpg?1783930706"
        ruling("2020-06-23", "Hooded Blightfang's triggered abilities trigger when it attacks and when it deals damage to a planeswalker, as long as it still has deathtouch.")
        ruling("2020-06-23", "In a Two-Headed Giant game, Hooded Blightfang's middle ability causes the opposing team to lose 2 life and you to gain 1 life.")
    }
}
