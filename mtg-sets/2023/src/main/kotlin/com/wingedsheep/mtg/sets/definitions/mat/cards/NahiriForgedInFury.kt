package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Nahiri, Forged in Fury — March of the Machine: The Aftermath #36
 * {4}{R}{W} · Legendary Creature — Kor Artificer · 5/4
 *
 * Affinity for Equipment
 * Whenever an equipped creature you control attacks, exile the top card of your library. You may
 * play that card this turn. You may cast Equipment spells this way without paying their mana costs.
 *
 * The attack trigger is a filtered `Triggers.a(...).attacks()` over equipped creatures you control,
 * so it fires once per equipped attacker (Nahiri included, if she's equipped). The exiled card gets a
 * this-turn may-play permission; only an Equipment card additionally gets the free-cast permission,
 * so any other card is played by paying its costs under normal timing rules (the Narset shape).
 */
val NahiriForgedInFury = card("Nahiri, Forged in Fury") {
    manaCost = "{4}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Kor Artificer"
    power = 5
    toughness = 4
    oracleText = "Affinity for Equipment (This spell costs {1} less to cast for each Equipment you control.)\n" +
        "Whenever an equipped creature you control attacks, exile the top card of your library. " +
        "You may play that card this turn. You may cast Equipment spells this way without paying their mana costs."

    keywordAbility(KeywordAbility.AffinityForSubtype(Subtype.EQUIPMENT))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().equipped()).attacks()
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(1))
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(exiled))
            val equipment = filter(exiled, GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT))
            run(Effects.GrantPlayWithoutPayingCost(equipment))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "36"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90eb0859-d24e-4703-8930-1cd4ac1fb3b6.jpg?1783916513"
        ruling("2023-05-12", "Nahiri's last ability will trigger once for each equipped creature you control that attacks, no matter how many Equipment are attached to any of them.")
        ruling("2023-05-12", "You may play the card exiled with Nahiri's ability even if Nahiri or the equipped creature leaves the battlefield or you lose control of them.")
        ruling("2023-05-12", "Playing the exiled card follows the normal rules for playing that card. Unless you're casting an Equipment spell this way, you must pay the spell's costs, and you must follow all applicable timing rules.")
        ruling("2023-05-12", "Unless an effect allows you to play additional lands that turn, you can play a land card exiled with Nahiri only if you haven't played a land yet that turn.")
        ruling("2023-05-12", "If you don't play the card, it will remain exiled.")
    }
}
