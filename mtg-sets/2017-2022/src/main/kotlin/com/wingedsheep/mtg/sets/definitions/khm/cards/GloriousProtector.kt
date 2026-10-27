package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glorious Protector (Kaldheim #12)
 * {2}{W}{W} · Creature — Angel Cleric 3/4
 * Flash
 * Flying
 * When this creature enters, you may exile any number of non-Angel creatures you control until
 * this creature leaves the battlefield.
 * Foretell {2}{W}
 *
 * The enters trigger doesn't target (2021-02-05 ruling): the choice is made on resolution, so it's a
 * pipeline that gathers your non-Angel creatures and lets you pick any number of them on the
 * battlefield — picking none is the "you may". Each pick is exiled with [Effects.ExileUntilLeaves],
 * which links it to this creature and is a no-op if it already left the battlefield before the
 * trigger resolved (the second ruling). The leaves-the-battlefield half is the usual linked return
 * under the owners' control; exiled tokens cease to exist and so never come back.
 */
val GloriousProtector = card("Glorious Protector") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Cleric"
    power = 3
    toughness = 4
    oracleText = "Flash\nFlying\n" +
        "When this creature enters, you may exile any number of non-Angel creatures you control " +
        "until this creature leaves the battlefield.\n" +
        "Foretell {2}{W} (During your turn, you may pay {2} and exile this card from your hand face down. Cast it on a later turn for its foretell cost.)"

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val yours = gather(
                CardSource.ControlledPermanents(
                    player = Player.You,
                    filter = GameObjectFilter.Creature.notSubtype(Subtype.ANGEL)
                )
            )
            val exiled = chooseAnyNumber(
                from = yours,
                chooser = Chooser.Controller,
                useTargetingUI = true,
                prompt = "Exile any number of non-Angel creatures you control until Glorious Protector leaves the battlefield"
            )
            run(Effects.ForEachInCollection(exiled, Effects.ExileUntilLeaves(EffectTarget.IterationEntity)))
        }
        description = "When this creature enters, you may exile any number of non-Angel creatures " +
            "you control until this creature leaves the battlefield."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    keywordAbility(KeywordAbility.foretell("{2}{W}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "PINDURSKI"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9f08552-7f1a-4fdb-9a70-d99cdbf75910.jpg?1783928284"
        ruling(
            "2021-02-05",
            "Glorious Protector's enters-the-battlefield ability doesn't target any creatures. You " +
                "choose which non-Angel creatures to exile, if any, as that ability resolves."
        )
        ruling(
            "2021-02-05",
            "If Glorious Protector leaves the battlefield before its triggered ability resolves, no " +
                "creatures will be exiled."
        )
        ruling(
            "2021-02-05",
            "If a token is exiled this way, it will cease to exist and won't return to the battlefield."
        )
    }
}
