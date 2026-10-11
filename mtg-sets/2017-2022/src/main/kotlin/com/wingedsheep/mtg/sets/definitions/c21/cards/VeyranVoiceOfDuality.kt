package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCastOrCopyTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Veyran, Voice of Duality — Commander 2021 #3
 * {1}{U}{R} · Legendary Creature — Efreet Wizard · 2/2
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, Veyran gets +1/+1 until end of turn.
 * If you casting or copying an instant or sorcery spell causes a triggered ability of a permanent you
 * control to trigger, that ability triggers an additional time.
 *
 * The second ability is [AdditionalCastOrCopyTriggers]; it doubles Veyran's own magecraft too.
 */
val VeyranVoiceOfDuality = card("Veyran, Voice of Duality") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Efreet Wizard"
    power = 2
    toughness = 2
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, Veyran gets +1/+1 until end of turn.\n" +
        "If you casting or copying an instant or sorcery spell causes a triggered ability of a permanent you " +
        "control to trigger, that ability triggers an additional time."

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, Veyran gets +1/+1 until end of turn."
    }

    staticAbility {
        ability = AdditionalCastOrCopyTriggers(spellFilter = GameObjectFilter.InstantOrSorcery)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56d93855-c749-4bdf-9a90-4f69f4226e46.jpg?1783927615"
        ruling("2021-04-16", "Veyran, Voice of Duality's last ability causes each of your magecraft abilities, including Veyran's own, to trigger an additional time.")
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "Veyran's effect doesn't copy the triggered ability; it just causes the ability to trigger again. Any choices you make as you put the ability on the stack, such as modes and targets, are made separately for each instance of the ability. Any choices made on resolution are also made individually.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
        ruling("2021-04-16", "If you somehow control two Veyrans (which would be poetic), you casting an instant or sorcery spell causes abilities to trigger three times. A third Veyran (less poetic) causes abilities to trigger four times, and so on.")
    }
}
