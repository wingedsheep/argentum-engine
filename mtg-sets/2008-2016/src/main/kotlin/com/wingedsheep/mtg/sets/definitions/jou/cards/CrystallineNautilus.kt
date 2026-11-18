package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crystalline Nautilus — Skulking Ghost's becomes-the-target self-sacrifice, twice. The printed
 * trigger works whether the Nautilus is a creature or a bestowed Aura (ruling), so targeting the
 * Aura sacrifices only the Aura. Bestowed, the enchanted creature also gets the same trigger
 * (default attached-creature scope), where "it" is that creature.
 */
val CrystallineNautilus = card("Crystalline Nautilus") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Nautilus"
    power = 4
    toughness = 4
    oracleText = "Bestow {3}{U}{U} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "When this permanent becomes the target of a spell or ability, sacrifice it.\n" +
        "Enchanted creature gets +4/+4 and has \"When this creature becomes the target of a spell or ability, sacrifice it.\""

    keywordAbility(KeywordAbility.bestow("{3}{U}{U}"))

    triggeredAbility {
        trigger = Triggers.self.becomesTarget()
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    }

    staticAbility { ability = ModifyStats(4, 4) }
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.becomesTarget(),
                effect = Effects.SacrificeTarget(EffectTarget.Self),
                descriptionOverride = "When this creature becomes the target of a spell or ability, sacrifice it.",
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "34"
        artist = "Brad Rigney"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88a60d25-b97f-40e7-a5ac-c456cee02aeb.jpg?1783939449"
        ruling("2014-04-26", "Crystalline Nautilus's middle ability functions as long as Crystalline Nautilus is on the battlefield, whether it's a creature or an Aura.")
        ruling("2014-04-26", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2014-04-26", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature.")
        ruling("2014-04-26", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
    }
}
