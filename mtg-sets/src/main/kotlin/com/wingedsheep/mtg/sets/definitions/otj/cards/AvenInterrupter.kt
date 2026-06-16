package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Aven Interrupter
 * {1}{W}{W}
 * Creature — Bird Rogue
 * 2/2
 *
 * Flash
 * Flying
 * When this creature enters, exile target spell. It becomes plotted. (Its owner may cast it
 * as a sorcery on a later turn without paying its mana cost.)
 * Spells your opponents cast from graveyards or from exile cost {2} more to cast.
 *
 * The ETB ability uses [Effects.ExileTargetSpell] with `makePlotted = true` — a non-counter
 * removal (CR 718; ruling: "Spells that can't be countered can still be exiled. They won't
 * resolve."). The exiled spell card becomes plotted for its **owner**, who may cast it for free
 * on a later turn. The static tax is a [ModifySpellCost] with the new
 * [SpellCostTarget.OpponentsCastFromZones] target — spells the controller's opponents cast from
 * a graveyard or from exile cost {2} more.
 */
val AvenInterrupter = card("Aven Interrupter") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Rogue"
    power = 2
    toughness = 2
    oracleText = "Flash\nFlying\n" +
        "When this creature enters, exile target spell. It becomes plotted. (Its owner may cast " +
        "it as a sorcery on a later turn without paying its mana cost.)\n" +
        "Spells your opponents cast from graveyards or from exile cost {2} more to cast."

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target("spell", Targets.Spell)
        effect = Effects.ExileTargetSpell(makePlotted = true)
        description = "When this creature enters, exile target spell. It becomes plotted."
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCastFromZones(setOf(Zone.GRAVEYARD, Zone.EXILE)),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Daniel Romanovsky"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3ca43a4-d194-440f-8099-f1fa103a108d.jpg?1712355236"

        ruling("2024-04-12", "Spells that can't be countered can still be exiled by Aven Interrupter's triggered ability. They won't resolve.")
        ruling("2024-04-12", "If the plotted card's owner casts it, the spell has no relation to the spell that player originally cast. Any choices made for the original spell aren't carried over to the new one.")
        ruling("2024-04-12", "You can't cast a plotted card on the same turn it became plotted.")
    }
}
