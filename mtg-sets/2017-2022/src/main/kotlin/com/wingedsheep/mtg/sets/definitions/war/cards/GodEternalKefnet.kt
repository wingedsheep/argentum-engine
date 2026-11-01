package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RevealFirstDrawEachTurn
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * God-Eternal Kefnet — War of the Spark #53 (canonical printing)
 * {2}{U}{U} · Legendary Creature — Zombie God · 4/5
 *
 * Flying
 * You may reveal the first card you draw each turn as you draw it. Whenever you reveal an instant
 * or sorcery card this way, copy that card and you may cast the copy. That copy costs {2} less to
 * cast.
 * When God-Eternal Kefnet dies or is put into exile from the battlefield, you may put it into its
 * owner's library third from the top.
 *
 * The reveal is [RevealFirstDrawEachTurn] with `optional = true`: the draw asks as the card is
 * drawn, before any later card of the same instruction. The "this way" trigger is linked to
 * Kefnet's own reveal (CR 607.1). The copy is made in the hand ([Effects.CopyCardIntoCollection]
 * creates it in the original's zone) and cast from there with `costReduction = 2`; one not cast
 * ceases to exist (the copy-of-a-card state-based action). "Dies or is put into exile" is two
 * triggers, God-Eternal Bontu's shape.
 */
val GodEternalKefnet = card("God-Eternal Kefnet") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Zombie God"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "You may reveal the first card you draw each turn as you draw it. Whenever you reveal an instant or " +
        "sorcery card this way, copy that card and you may cast the copy. That copy costs {2} less to cast.\n" +
        "When God-Eternal Kefnet dies or is put into exile from the battlefield, you may put it into its " +
        "owner's library third from the top."

    keywords(Keyword.FLYING)

    staticAbility { ability = RevealFirstDrawEachTurn(optional = true) }

    triggeredAbility {
        trigger = Triggers.you.revealsFirstDraw(GameObjectFilter.InstantOrSorcery)
        effect = Effects.Pipeline {
            val copy = copyCard(EffectTarget.TriggeringEntity)
            run(Effects.May(
                Effects.CastFromCollection(copy, costReduction = 2),
                descriptionOverride = "You may cast the copy. It costs {2} less to cast.",
            ))
        }
        description = "Whenever you reveal an instant or sorcery card this way, copy that card and you may " +
            "cast the copy. That copy costs {2} less to cast."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.GRAVEYARD, positionFromTop = 2),
            prompt = "Put God-Eternal Kefnet into its owner's library third from the top?"
        )
        description = "When God-Eternal Kefnet dies, you may put it into its owner's library third from the top."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves(to = Zone.EXILE)
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.EXILE, positionFromTop = 2),
            prompt = "Put God-Eternal Kefnet into its owner's library third from the top?"
        )
        description = "When God-Eternal Kefnet is put into exile from the battlefield, you may put it into its " +
            "owner's library third from the top."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "53"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e14060f-9167-4e28-a053-77689e066e30.jpg?1783933465"

        ruling(
            "2019-05-03",
            "It's important to reveal the first card you draw each turn (or choose not to reveal it) before it is " +
                "mixed with the other cards in your hand. You look at the card as you draw it before choosing " +
                "whether to reveal it."
        )
        ruling(
            "2019-05-03",
            "Multiple card draws are always treated as a sequence of individual card draws. Only the first card " +
                "drawn this way may be revealed and copied with Kefnet's ability."
        )
        ruling(
            "2019-05-03",
            "You can cast the copy only as Kefnet's triggered ability resolves. If you don't want to cast it at " +
                "that time (or you can't cast it), the copy ceases to exist."
        )
        ruling("2019-05-03", "The copy is created in and cast from your hand.")
    }
}
